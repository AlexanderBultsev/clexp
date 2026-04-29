package com.clexp.post.repository;

import java.util.UUID;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import com.clexp.post.model.PostInterest;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface PostInterestRepository extends R2dbcRepository<PostInterest, UUID> {
    
    Flux<PostInterest> findAllByPostId(UUID postId);

    Mono<Void> deleteAllByPostId(UUID postId);
}
