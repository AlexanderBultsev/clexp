package com.clexp.recm.service;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import com.clexp.common.model.Interest;
import com.clexp.post.model.Post;

/**
 * Сервис подсчёта score релевантности
 *
 * === SCORING ДЛЯ ПОСТОВ ===
 *
 * score(post, user) = tagScore + contentScore + likedSimilarityScore
 *
 * tagScore — +TAG_MATCH_WEIGHT за каждый Interest-тег на посте, совпадающий с интересами
 * пользователя (из post_interests). Максимально точный сигнал, т.к. тег выставлен явно.
 *
 * contentScore — +CONTENT_KEYWORD_WEIGHT за каждое слово из Interest.name, найденное в тексте
 * content поста (case-insensitive). Позволяет находить релевантные посты без тегов.
 *
 * likedSimilarityScore — +LIKED_SIMILARITY_WEIGHT * jaccardSimilarity(post, likedPost) Jaccard
 * similarity считается по множеству слов content. Берём максимум по всем лайкнутым постам (наиболее
 * похожий). Позволяет рекомендовать похожие на уже понравившиеся посты.
 *
 * === SCORING ДЛЯ ПОЛЬЗОВАТЕЛЕЙ ===
 *
 * score(candidate, current) = languageScore + interestScore
 *
 * languageScore — +LANGUAGE_MATCH_WEIGHT если выполняется условие: current.KNOWN ∩
 * candidate.LEARNING ≠ ∅ (текущий знает то, что учит кандидат) ИЛИ current.LEARNING ∩
 * candidate.KNOWN ≠ ∅ (кандидат знает то, что учит текущий) + смена ролей LOCAL ↔ TOURIST
 * учитывается в UserRecommendationService.
 *
 * interestScore — +INTEREST_MATCH_WEIGHT за каждый общий Interest между пользователями.
 */
@Service
public class ScoringService {

    // Веса для постов

    /** Вес за явный тег интереса на посте */
    public static final double TAG_MATCH_WEIGHT = 3.0;

    /** Вес за каждое ключевое слово интереса, найденное в тексте поста */
    public static final double CONTENT_KEYWORD_WEIGHT = 1.0;

    /** Максимальный вес за схожесть с лайкнутыми постами */
    public static final double LIKED_SIMILARITY_WEIGHT = 2.0;

    // Веса для пользователей

    /** Вес за языковую совместимость */
    public static final double LANGUAGE_MATCH_WEIGHT = 5.0;

    /** Вес за каждый общий интерес между пользователями */
    public static final double INTEREST_MATCH_WEIGHT = 2.0;

    // Scoring постов

    /**
     * Считает score поста на основе тегов интересов.
     *
     * @param postInterestIds UUID интересов, которыми помечен пост
     * @param userInterestIds UUID интересов пользователя
     * @return score
     */
    public double scoreByInterestTags(Set<UUID> postInterestIds, Set<UUID> userInterestIds) {
        if (postInterestIds.isEmpty() || userInterestIds.isEmpty()) {
            return 0.0;
        }
        long matchCount = postInterestIds.stream().filter(userInterestIds::contains).count();
        double score = matchCount * TAG_MATCH_WEIGHT;
        return score;
    }

     /**
     * Считает score поста на основе ключевых слов интересов в тексте контента.
     * Для каждого интереса ищем все его слова в content (case-insensitive).
     * Если хотя бы одно слово нашли — засчитываем интерес.
     *
     * @param post             пост для оценки
     * @param userInterests    список интересов пользователя
     * @return score
     */
    public double scoreByContentKeywords(Post post, List<Interest> userInterests) {
        if (post.getContent() == null || post.getContent().isBlank() || userInterests.isEmpty()) {
            return 0.0;
        }
        String contentLower = post.getContent().toLowerCase();
        long matchCount = userInterests.stream()
                .filter(interest -> {
                    if (interest.getName() == null) return false;
                    // Разбиваем название интереса на слова и ищем каждое
                    String[] words = interest.getName().toLowerCase().split("\\P{L}+");
                    return Arrays.stream(words).anyMatch(contentLower::contains);
                })
                .count();
        double score = matchCount * CONTENT_KEYWORD_WEIGHT;
        return score;
    }

    /**
     * Считает score поста на основе схожести с лайкнутыми постами.
     * Использует метрику Jaccard Similarity по множеству слов контента.
     *
     * Берём максимальный Jaccard среди всех лайкнутых постов —
     * т.е. ищем наиболее похожий лайкнутый пост.
     *
     * @param post         пост для оценки
     * @param likedPosts   список постов, которые пользователь лайкнул
     * @return score
     */
    public double scoreByLikedSimilarity(Post post, List<Post> likedPosts) {
        if (likedPosts.isEmpty() || post.getContent() == null || post.getContent().isBlank()) {
            return 0.0;
        }
        Set<String> postWords = tokenize(post.getContent());
        if (postWords.isEmpty()) return 0.0;

        double maxSimilarity = likedPosts.stream()
                .filter(liked -> liked.getContent() != null && !liked.getContent().isBlank())
                // Не сравниваем пост сам с собой
                .filter(liked -> !liked.getId().equals(post.getId()))
                .mapToDouble(liked -> jaccardSimilarity(postWords, tokenize(liked.getContent())))
                .max()
                .orElse(0.0);

        double score = LIKED_SIMILARITY_WEIGHT * maxSimilarity;
        return score;
    }

    // Scoring пользователей

    /**
     * Считает score за общие интересы между двумя пользователями.
     *
     * @param currentUserInterestIds   UUID интересов текущего пользователя
     * @param candidateInterestIds     UUID интересов кандидата
     * @return score
     */
    public double scoreBySharedInterests(Set<UUID> currentUserInterestIds, Set<UUID> candidateInterestIds) {
        if (currentUserInterestIds.isEmpty() || candidateInterestIds.isEmpty()) {
            return 0.0;
        }
        long sharedCount = candidateInterestIds.stream()
                .filter(currentUserInterestIds::contains)
                .count();
        double score = sharedCount * INTEREST_MATCH_WEIGHT;
        return score;
    }

    /**
     * Считает score за языковую совместимость.
     * Логика:
     *   +LANGUAGE_MATCH_WEIGHT за каждый язык, который текущий пользователь ЗНАЕТ (KNOWN),
     *   а кандидат УЧИТ (LEARNING) — или наоборот.
     *
     * @param currentKnownLanguageIds   языки, которые текущий пользователь знает
     * @param currentLearningLanguageIds языки, которые текущий пользователь учит
     * @param candidateKnownLanguageIds  языки, которые кандидат знает
     * @param candidateLearningLanguageIds языки, которые кандидат учит
     * @return score
     */
    public double scoreByLanguageMatch(
            Set<UUID> currentKnownLanguageIds,
            Set<UUID> currentLearningLanguageIds,
            Set<UUID> candidateKnownLanguageIds,
            Set<UUID> candidateLearningLanguageIds) {

        double score = 0.0;

        // Текущий ЗНАЕТ → Кандидат УЧИТ
        long match1 = currentKnownLanguageIds.stream()
                .filter(candidateLearningLanguageIds::contains)
                .count();

        // Кандидат ЗНАЕТ → Текущий УЧИТ
        long match2 = candidateKnownLanguageIds.stream()
                .filter(currentLearningLanguageIds::contains)
                .count();

        score += (match1 + match2) * LANGUAGE_MATCH_WEIGHT;
        return score;
    }

    // Вспомогательные методы

    /**
     * Разбивает текст на множество уникальных слов (нижний регистр, только буквы).
     * Слова короче 3 символов игнорируются (артикли, предлоги и т.д.).
     */
    public Set<String> tokenize(String text) {
        if (text == null || text.isBlank()) return new HashSet<>();
        // TODO REGEX НЕ СРАБОТАЕТ ДЛЯ ДРУГИХ ЯЗЫКОВ
        return Arrays.stream(text.toLowerCase().split("\\P{L}+"))
                .filter(word -> word.length() >= 3)
                .collect(Collectors.toSet());
    }

    /**
     * Jaccard Similarity между двумя множествами слов.
     * Jaccard(A, B) = |A ∩ B| / |A ∪ B|
     * Возвращает значение от 0.0 (нет общих слов) до 1.0 (идентичные тексты).
     */
    public double jaccardSimilarity(Set<String> a, Set<String> b) {
        if (a.isEmpty() && b.isEmpty()) return 1.0;
        if (a.isEmpty() || b.isEmpty()) return 0.0;

        Set<String> intersection = new HashSet<>(a);
        intersection.retainAll(b);

        Set<String> union = new HashSet<>(a);
        union.addAll(b);

        return (double) intersection.size() / union.size();
    }
}
