package com.clexp.post.repository;

import java.util.UUID;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import com.clexp.post.model.Comment;
import reactor.core.publisher.Flux;

public interface CommentRepository extends R2dbcRepository<Comment, UUID> {
    Flux<Comment> findByPostId(UUID postId);
    Flux<Comment> findByUserId(UUID userId);
}
