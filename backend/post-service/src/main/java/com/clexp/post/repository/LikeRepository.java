package com.clexp.post.repository;

import java.util.UUID;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import com.clexp.post.model.Like;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface LikeRepository extends R2dbcRepository<Like, UUID> {
    Flux<Like> findByPostId(UUID postId);
    Flux<Like> findByUserId(UUID userId);
    Mono<Boolean> existsByUserIdAndPostId(UUID userId, UUID postId);
    Mono<Void> deleteByUserIdAndPostId(UUID userId, UUID postId);
}
