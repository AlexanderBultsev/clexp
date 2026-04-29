package com.clexp.recm.service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import com.clexp.common.model.Interest;
import com.clexp.common.repository.InterestRepository;
import com.clexp.post.dto.PostResponse;
import com.clexp.post.model.Post;
import com.clexp.post.model.PostInterest;
import com.clexp.post.repository.LikeRepository;
import com.clexp.post.repository.PostInterestRepository;
import com.clexp.post.repository.PostRepository;
import com.clexp.post.service.PostService;
import com.clexp.user.repository.SubscriptionRepository;
import com.clexp.user.repository.UserInterestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Сервис рекомендаций постов.
 *
 * Алгоритм (Content-based filtering + cursor-пагинация):
 *
 * 1. Получаем интересы текущего пользователя
 * 2. Получаем последние MAX_LIKED_POSTS лайкнутых постов пользователя
 * 3. Загружаем CANDIDATE_BATCH_SIZE чужих постов, созданных ДО момента cursor (createdAt < cursor),
 *    отсортированных по createdAt DESC — через репозиторий
 * 4. Исключаем лайкнутые посты из кандидатов
 * 5. Для каждого кандидата считаем:
 *    - relevanceScore = tagScore + contentScore + likedScore
 *    - recencyScore   = e^(-RECENCY_LAMBDA * daysSinceCreation)
 *    - score     = relevanceScore * RELEVANCE_WEIGHT + recencyScore * RECENCY_WEIGHT
 * 6. Фильтруем score == 0, сортируем DESC, берём топ MAX_POSTS_PER_PAGE
 * 7. Cursor для следующего запроса = createdAt последнего поста из пачки
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PostRecommendationService {

    /** Сколько постов отдаём клиенту за один запрос. */
    private static final int MAX_POSTS_PER_PAGE = 10;

    /** Сколько постов загружаем из БД за один запрос. */
    private static final int CANDIDATE_BATCH_SIZE = 40;

    /** Максимум лайкнутых постов для расчёта Jaccard similarity. */
    private static final int MAX_LIKED_POSTS = 50;

    /** Коэффициент затухания recencyScore: e^(-λ * days). */
    private static final double RECENCY_LAMBDA = 0.1;

    /** Вес relevanceScore в итоговой формуле. */
    private static final double RELEVANCE_WEIGHT = 0.7;

    /** Вес recencyScore в итоговой формуле. */
    private static final double RECENCY_WEIGHT = 0.3;

    //** Бонусный вес подписки */
    private static final double SUBSCRIPTION_BONUS = 1.0;


    private final PostService postService;
    private final LikeRepository likeRepository;
    private final PostRepository postRepository;
    private final PostInterestRepository postInterestRepository;
    private final UserInterestRepository userInterestRepository;
    private final InterestRepository interestRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final ScoringService scoringService;

    /**
     * Получить рекомендованные посты для пользователя.
     *
     * Cursor-пагинация: клиент передаёт createdAt последнего поста
     * из предыдущей пачки. При первом запросе передаёт null.
     *
     * @param currentUserId ID текущего пользователя
     * @param cursor        верхняя граница выборки по createdAt (exclusive), null для первого запроса
     * @return Flux топ-MAX_POSTS_PER_PAGE рекомендованных постов, отсортированных по finalScore DESC
     */
    public Mono<PostFeed> getRecommendedPosts(UUID currentUserId, LocalDateTime cursor) {
        LocalDateTime effectiveCursor = cursor != null ? cursor : LocalDateTime.now();
        
        log.info("Starting post recommendations for userId={}, cursor={}, effectiveCursor={}", 
                currentUserId, cursor, effectiveCursor);
        long startTime = System.currentTimeMillis();

        // 1. Загружаем интересы пользователя
        Mono<List<Interest>> userInterestsMono = userInterestRepository
                .findAllByUserId(currentUserId)
                .flatMap(userInterest -> interestRepository.findById(userInterest.getInterestId()))
                .collectList()
                .doOnSuccess(interests -> log.debug("Loaded {} user interests for userId={}", interests.size(), currentUserId))
                .doOnError(e -> log.error("Failed to load user interests for userId={}", currentUserId, e));

        // 2. Загружаем лайкнутые посты пользователя
        Mono<List<Post>> likedPostsMono = likeRepository
                .findTopLikesByUserId(currentUserId, MAX_LIKED_POSTS)
                .flatMap(userLikes -> postRepository.findById(userLikes.getPostId()))
                .collectList()
                .doOnSuccess(posts -> log.debug("Loaded {} liked posts for userId={}", posts.size(), currentUserId))
                .doOnError(e -> log.error("Failed to load liked posts for userId={}", currentUserId, e));

        // Загружаем ID пользователей, на которых текущий пользователь подписан
        Mono<Set<UUID>> followedUserIdsMono = subscriptionRepository
                .findAllBySourceId(currentUserId)
                .map(subscription -> subscription.getTargetId())
                .collect(Collectors.toSet())
                .doOnSuccess(ids -> log.debug("Loaded {} subscriptions for userId={}", ids.size(), currentUserId))
                .doOnError(e -> log.error("Failed to load subscriptions for userId={}", currentUserId, e));

        // 3. Загружаем CANDIDATE_BATCH_SIZE чужих постов до cursor включительно
        Mono<List<Post>> candidatePostsMono = postRepository
                .findCandidatesForFeed(currentUserId, effectiveCursor, CANDIDATE_BATCH_SIZE)
                .collectList()
                .doOnSuccess(posts -> log.debug("Loaded {} candidate posts for userId={}, effectiveCursor={}", 
                        posts.size(), currentUserId, effectiveCursor))
                .doOnError(e -> log.error("Failed to load candidate posts for userId={}", currentUserId, e));

        return Mono.zip(userInterestsMono, likedPostsMono, candidatePostsMono, followedUserIdsMono)
                .flatMap(tuple -> {
                    List<Interest> userInterests   = tuple.getT1();
                    List<Post>     likedPosts      = tuple.getT2();
                    List<Post>     candidatePosts  = tuple.getT3();
                    Set<UUID>     followedUserIds = tuple.getT4();

                    log.info("Processing recommendations: userInterests={}, likedPosts={}, candidates={}, subscriptions={}", 
                            userInterests.size(), likedPosts.size(), candidatePosts.size(), followedUserIds.size());

                    LocalDateTime nextCursor = candidatePosts.isEmpty() ? null
                            : candidatePosts.get(candidatePosts.size() - 1).getCreatedAt();
                
                    Set<UUID> userInterestIds = userInterests.stream()
                            .map(Interest::getId)
                            .collect(Collectors.toSet());

                    // Исключаем уже лайкнутые посты из рекомендаций
                    Set<UUID> likedPostIds = likedPosts.stream()
                            .map(Post::getId)
                            .collect(Collectors.toSet());

                    List<Post> filteredCandidates = candidatePosts.stream()
                            .filter(post -> !likedPostIds.contains(post.getId()))
                            .collect(Collectors.toList());

                    log.debug("Filtered candidates: {} posts (excluded {} already liked)", 
                            filteredCandidates.size(), candidatePosts.size() - filteredCandidates.size());

                    // 4. Для каждого кандидата загружаем его теги и считаем score
                    return Flux.fromIterable(filteredCandidates)
                            .flatMap(post -> scorePost(post, userInterests, userInterestIds, likedPosts, followedUserIds))
                            .doOnNext(scored -> {
                                if (log.isTraceEnabled()) {
                                    log.trace("Scored post: id={}, score={:.4f}", scored.post.getId(), scored.score);
                                }
                            })
                            .filter(scored -> {
                                boolean passed = scored.score > 0.0;
                                if (!passed && log.isTraceEnabled()) {
                                    log.trace("Filtered out post with zero score: id={}", scored.post.getId());
                                }
                                return passed;
                            })
                            .sort(Comparator.comparingDouble(scored -> -scored.score))
                            .take(MAX_POSTS_PER_PAGE)
                            .doOnNext(scored -> log.debug("Top post: id={}, score={:.4f}", scored.post.getId(), scored.score))
                            .flatMap(scored -> postService.mapToPostResponse(scored.post, currentUserId))
                            .collectList()
                            .map(posts -> {
                                long duration = System.currentTimeMillis() - startTime;
                                log.info("Post recommendations completed for userId={}: returned {} posts, nextCursor={}, duration={}ms", 
                                        currentUserId, posts.size(), nextCursor, duration);
                                return new PostFeed(posts, nextCursor);
                            });
                })
                .doOnError(e -> {
                    long duration = System.currentTimeMillis() - startTime;
                    log.error("Post recommendations failed for userId={}, duration={}ms", currentUserId, duration, e);
                });
    }

    /**
     * Считает score для одного поста.
     *
     * @return Mono с внутренним record ScoredPost (post + score)
     */
    private Mono<ScoredPost> scorePost(
            Post            post,
            List<Interest>  userInterests,
            Set<UUID>       userInterestIds,
            List<Post>      likedPosts,
            Set<UUID>       followedUserIds) {

        log.trace("Scoring post: id={}, userId={}", post.getId(), post.getUserId());

        return postInterestRepository.findAllByPostId(post.getId())
                .map(PostInterest::getInterestId)
                .collect(Collectors.toSet())
                .map(postInterestIds -> {
                    double tagScore      = scoringService.scoreByInterestTags(postInterestIds, userInterestIds);
                    double contentScore  = scoringService.scoreByContentKeywords(post, userInterests);
                    double likedScore    = scoringService.scoreByLikedSimilarity(post, likedPosts);
                    double subscriptionBonus = followedUserIds.contains(post.getUserId()) ? SUBSCRIPTION_BONUS : 0.0;

                    double relevance     = tagScore + contentScore + likedScore + subscriptionBonus;

                    double daysSinceCreation = ChronoUnit.DAYS.between(post.getCreatedAt(), LocalDateTime.now());
                    double recency           = Math.exp(-RECENCY_LAMBDA * daysSinceCreation);

                    double score = relevance * RELEVANCE_WEIGHT + recency * RECENCY_WEIGHT;

                    if (log.isTraceEnabled()) {
                        log.trace("Post score breakdown for id={}: tagScore={:.3f}, contentScore={:.3f}, " +
                                "likedScore={:.3f}, subBonus={:.3f}, relevance={:.3f}, recency={:.3f} (days={}), " +
                                "finalScore={:.4f}", 
                                post.getId(), tagScore, contentScore, likedScore, subscriptionBonus, 
                                relevance, recency, daysSinceCreation, score);
                    }

                    return new ScoredPost(post, score);
                })
                .doOnError(e -> log.error("Failed to score post: id={}", post.getId(), e));
    }

    private record ScoredPost(Post post, double score) {}

    public record PostFeed(List<PostResponse> posts, LocalDateTime nextCursor) {}
}