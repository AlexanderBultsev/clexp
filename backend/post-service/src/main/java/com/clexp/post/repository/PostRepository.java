package com.clexp.post.repository;

import java.util.UUID;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import com.clexp.post.model.Post;
import reactor.core.publisher.Flux;

public interface PostRepository extends R2dbcRepository<Post, UUID> {
    Flux<Post> findByUserId(UUID userId);
}
