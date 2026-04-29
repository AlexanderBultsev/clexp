package com.clexp.post.repository;

import java.util.UUID;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import com.clexp.post.model.Like;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface LikeRepository extends R2dbcRepository<Like, UUID> {
    Flux<Like> findAllByPostId(UUID postId);
    Flux<Like> findAllByUserId(UUID userId);
    Mono<Boolean> existsByUserIdAndPostId(UUID userId, UUID postId);
    Mono<Void> deleteByUserIdAndPostId(UUID userId, UUID postId);

    @Query("""
            SELECT *
            FROM likes
            WHERE user_id = :userId
            ORDER BY created_at DESC
            LIMIT :limit
            """)
    Flux<Like> findTopLikesByUserId(
            @Param("currentUserId") UUID currentUserId,
            @Param("limit")         int  limit);
}
