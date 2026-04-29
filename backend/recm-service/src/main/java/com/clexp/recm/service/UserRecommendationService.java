package com.clexp.recm.service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import com.clexp.auth.repository.UserRepository;
import com.clexp.common.model.User;
import com.clexp.common.model.UserRole;
import com.clexp.user.dto.UserResponse;
import com.clexp.user.model.UserLanguageStatus;
import com.clexp.user.repository.UserInterestRepository;
import com.clexp.user.repository.UserLanguageRepository;
import com.clexp.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Сервис рекомендаций пользователей.
 *
 * Алгоритм (Content-based filtering):
 *
 * 1. Получаем профиль текущего пользователя (роль, языки, интересы)
 * 2. Загружаем CANDIDATE_BATCH_SIZE других пользователей
 * 3. Для каждого кандидата считаем score:
 *    - scoreByLanguageMatch — языковую совместимость
 *    - scoreBySharedInterests — общие интересы
 * 4. Языковая совместимость учитывает роли LOCAL - TOURIST
 * 5. Фильтруем кандидатов с score == 0
 * 6. Сортируем по score DESC, берём топ MAX_RECOMMENDATIONS
 *
 * === Матрица ролей и языков ===
 *
 *  Текущий       Кандидат      Языковой матч
 *  ─────────────────────────────────────────────────────
 *  LOCAL (KNOWN X)   TOURIST (LEARNING X)  ✓  +LANGUAGE_WEIGHT * роловый бонус
 *  LOCAL (LEARNING X) TOURIST (KNOWN X)    ✓  +LANGUAGE_WEIGHT * роловый бонус
 *  TOURIST (KNOWN X)  LOCAL (LEARNING X)   ✓  +LANGUAGE_WEIGHT * роловый бонус
 *  TOURIST (LEARNING X) LOCAL (KNOWN X)    ✓  +LANGUAGE_WEIGHT * роловый бонус
 *  LOCAL (KNOWN X)   LOCAL (LEARNING X)    ✓  +LANGUAGE_WEIGHT (без бонуса)
 *  TOURIST (KNOWN X) TOURIST (LEARNING X)  ✓  +LANGUAGE_WEIGHT (без бонуса)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserRecommendationService {

    /** Сколько пользователей отдаём клиенту за один запрос. */
    private static final int MAX_USERS_PER_PAGE = 5;

    /** Сколько пользователей загружаем из БД за один запрос. */
    private static final int CANDIDATE_BATCH_SIZE = 10;

    /** Множитель для ролевого бонуса */
    private static final double ROLE_COMPLEMENT_MULTIPLIER = 1.5;

    private final UserService userService;
    private final UserRepository userRepository;
    private final UserInterestRepository userInterestRepository;
    private final UserLanguageRepository userLanguageRepository;
    private final ScoringService scoringService;

    /**
     * Получить рекомендованных пользователей для текущего пользователя.
     *
     * @param currentUserId ID текущего пользователя
     * @param cursor        верхняя граница выборки по createdAt (exclusive), null для первого запроса
     * @return Flux топ-MAX_USERS_PER_PAGE рекомендованных пользователей, отсортированных по score DESC
     */
    public Mono<UserFeed> getRecommendedUsers(UUID currentUserId, LocalDateTime cursor) {
        LocalDateTime effectiveCursor = cursor != null ? cursor : LocalDateTime.now();
        
        log.info("Starting user recommendations for userId={}, cursor={}, effectiveCursor={}", 
                currentUserId, cursor, effectiveCursor);
        long startTime = System.currentTimeMillis();

        // 1. Загружаем профиль текущего пользователя
        Mono<User> currentUserMono = userRepository.findById(currentUserId)
                .doOnSuccess(user -> log.debug("Loaded current user: id={}, role={}", user.getId(), user.getRole()))
                .doOnError(e -> log.error("Failed to load current user: id={}", currentUserId, e));

        // Загружаем языки текущего пользователя
        Mono<Set<UUID>> currentKnownLangsMono = userLanguageRepository
                .findAllByUserIdAndStatus(currentUserId, UserLanguageStatus.KNOWN)
                .map(ul -> ul.getLanguageId())
                .collect(Collectors.toSet())
                .doOnSuccess(langs -> log.debug("Loaded {} KNOWN languages for userId={}", langs.size(), currentUserId))
                .doOnError(e -> log.error("Failed to load KNOWN languages for userId={}", currentUserId, e));
        
        Mono<Set<UUID>> currentLearningLangsMono = userLanguageRepository
                .findAllByUserIdAndStatus(currentUserId, UserLanguageStatus.LEARNING)
                .map(ul -> ul.getLanguageId())
                .collect(Collectors.toSet())
                .doOnSuccess(langs -> log.debug("Loaded {} LEARNING languages for userId={}", langs.size(), currentUserId))
                .doOnError(e -> log.error("Failed to load LEARNING languages for userId={}", currentUserId, e));
        
        // Загружаем интересы текущего пользователя
        Mono<Set<UUID>> currentInterestsMono = userInterestRepository
                .findAllByUserId(currentUserId)
                .map(ui -> ui.getInterestId())
                .collect(Collectors.toSet())
                .doOnSuccess(interests -> log.debug("Loaded {} interests for userId={}", interests.size(), currentUserId))
                .doOnError(e -> log.error("Failed to load interests for userId={}", currentUserId, e));
        
        // 3. Загружаем CANDIDATE_BATCH_SIZE других пользователей до cursor включительно
        Mono<List<User>> candidateUsersMono = userRepository
                .findCandidatesForFeed(currentUserId, effectiveCursor, CANDIDATE_BATCH_SIZE)
                .collectList()
                .doOnSuccess(users -> log.debug("Loaded {} candidate users for userId={}, effectiveCursor={}", 
                        users.size(), currentUserId, effectiveCursor))
                .doOnError(e -> log.error("Failed to load candidate users for userId={}", currentUserId, e));
        
        return Mono.zip(currentUserMono, currentKnownLangsMono, currentLearningLangsMono, currentInterestsMono, candidateUsersMono)
                .flatMap(tuple -> {
                    User       currentUser          = tuple.getT1();
                    Set<UUID>  currentKnownLangs    = tuple.getT2();
                    Set<UUID>  currentLearningLangs = tuple.getT3();
                    Set<UUID>  currentInterests     = tuple.getT4();
                    List<User> candidateUsers       = tuple.getT5();

                    log.info("Processing recommendations: role={}, knownLangs={}, learningLangs={}, interests={}, candidates={}", 
                            currentUser.getRole(), currentKnownLangs.size(), currentLearningLangs.size(), 
                            currentInterests.size(), candidateUsers.size());

                    LocalDateTime nextCursor = candidateUsers.isEmpty() ? null
                            : candidateUsers.get(candidateUsers.size() - 1).getCreatedAt();

                    // 4. Для каждого кандидата считаем score
                    return Flux.fromIterable(candidateUsers)
                            .flatMap(candidate -> scoreUser(
                                candidate,
                                currentUser,
                                currentKnownLangs,
                                currentLearningLangs,
                                currentInterests))
                            .doOnNext(scored -> {
                                if (log.isTraceEnabled()) {
                                    log.trace("Scored user: id={}, role={}, score={:.4f}", 
                                            scored.user.getId(), scored.user.getRole(), scored.score);
                                }
                            })
                            .filter(scored -> {
                                boolean passed = scored.score > 0.0;
                                if (!passed && log.isTraceEnabled()) {
                                    log.trace("Filtered out user with zero score: id={}", scored.user.getId());
                                }
                                return passed;
                            })
                            .sort(Comparator.comparingDouble(scored -> -scored.score))
                            .take(MAX_USERS_PER_PAGE)
                            .doOnNext(scored -> log.debug("Top user: id={}, role={}, score={:.4f}", 
                                    scored.user.getId(), scored.user.getRole(), scored.score))
                            .flatMap(scored -> userService.mapToUserResponse(scored.user, currentUserId))
                            .collectList()
                            .map(users -> {
                                long duration = System.currentTimeMillis() - startTime;
                                log.info("User recommendations completed for userId={}: returned {} users, nextCursor={}, duration={}ms", 
                                        currentUserId, users.size(), nextCursor, duration);
                                return new UserFeed(users, nextCursor);
                            });
                })
                .doOnError(e -> {
                    long duration = System.currentTimeMillis() - startTime;
                    log.error("User recommendations failed for userId={}, duration={}ms", currentUserId, duration, e);
                });
    }

    /**
     * Считает finalScore для одного пользователя.
     *
     * @return Mono с внутренним record ScoredUser (user + finalScore)
     */
    private Mono<ScoredUser> scoreUser(
            User candidate,
            User currentUser,
            Set<UUID> currentKnownLangs,
            Set<UUID> currentLearningLangs,
            Set<UUID> currentInterests) {

        log.trace("Scoring user: id={}, role={}", candidate.getId(), candidate.getRole());

        // Загружаем данные кандидата параллельно
        Mono<Set<UUID>> candidateKnownLangsMono = userLanguageRepository
                .findAllByUserIdAndStatus(candidate.getId(), UserLanguageStatus.KNOWN)
                .map(ul -> ul.getLanguageId())
                .collect(Collectors.toSet())
                .doOnSuccess(langs -> {
                    if (log.isTraceEnabled()) {
                        log.trace("Candidate KNOWN languages: {}", langs.size());
                    }
                });

        Mono<Set<UUID>> candidateLearningLangsMono = userLanguageRepository
                .findAllByUserIdAndStatus(candidate.getId(), UserLanguageStatus.LEARNING)
                .map(ul -> ul.getLanguageId())
                .collect(Collectors.toSet())
                .doOnSuccess(langs -> {
                    if (log.isTraceEnabled()) {
                        log.trace("Candidate LEARNING languages: {}", langs.size());
                    }
                });

        Mono<Set<UUID>> candidateInterestMono = userInterestRepository
                .findAllByUserId(candidate.getId())
                .map(ui -> ui.getInterestId())
                .collect(Collectors.toSet())
                .doOnSuccess(interests -> {
                    if (log.isTraceEnabled()) {
                        log.trace("Candidate interests: {}", interests.size());
                    }
                });

        return Mono.zip(candidateKnownLangsMono, candidateLearningLangsMono, candidateInterestMono)
                .map(tuple -> {
                    Set<UUID> candidateKnownLangs    = tuple.getT1();
                    Set<UUID> candidateLearningLangs = tuple.getT2();
                    Set<UUID> candidateInterest      = tuple.getT3();

                    double rawLanguageScore = scoringService.scoreByLanguageMatch(currentKnownLangs,
                            currentLearningLangs, candidateKnownLangs, candidateLearningLangs);

                    double roleMultiplier = getRoleMultiplier(currentUser.getRole(), candidate.getRole());
                    double languageScore = rawLanguageScore * roleMultiplier;

                    double interestScore = scoringService.scoreBySharedInterests(currentInterests, candidateInterest);

                    double score = languageScore + interestScore;

                    if (log.isTraceEnabled()) {
                        log.trace("User score breakdown for id={}: rawLangScore={:.3f}, roleMultiplier={:.2f}, " +
                                "langScore={:.3f}, interestScore={:.3f}, finalScore={:.4f}, " +
                                "roles=[current={}, candidate={}]", 
                                candidate.getId(), rawLanguageScore, roleMultiplier, languageScore, 
                                interestScore, score, currentUser.getRole(), candidate.getRole());
                    }

                    return new ScoredUser(candidate, score);
                })
                .doOnError(e -> log.error("Failed to score user: id={}", candidate.getId(), e));
    }

    /**
     * Возвращает множитель для языкового score на основе ролей.
     *
     * LOCAL - TOURIST - максимальный бонус
     * LOCAL - LOCAL или TOURIST - TOURIST - без бонуса
     */
    private double getRoleMultiplier(UserRole currentRole, UserRole candidateRole) {
        if (currentRole == null || candidateRole == null) {
            log.trace("Role multiplier: one or both roles are null, returning 1.0");
            return 1.0;
        }
        boolean complementaryRoles = (currentRole == UserRole.LOCAL && candidateRole == UserRole.TOURIST)
                || (currentRole == UserRole.TOURIST && candidateRole == UserRole.LOCAL);
        
        double multiplier = complementaryRoles ? ROLE_COMPLEMENT_MULTIPLIER : 1.0;
        
        if (log.isTraceEnabled()) {
            log.trace("Role multiplier: current={}, candidate={}, complementary={}, multiplier={:.2f}", 
                    currentRole, candidateRole, complementaryRoles, multiplier);
        }
        
        return multiplier;
    }

    private record ScoredUser(User user, double score) {}

    public record UserFeed(List<UserResponse> users, LocalDateTime nextCursor) {
    }
}