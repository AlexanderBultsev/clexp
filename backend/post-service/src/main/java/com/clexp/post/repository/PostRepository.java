package com.clexp.post.repository;

import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import com.clexp.post.model.Post;
import reactor.core.publisher.Flux;

public interface PostRepository extends R2dbcRepository<Post, UUID> {

    Flux<Post> findAllByUserId(UUID userId);

    @Query("""
            SELECT *
            FROM posts
            WHERE user_id != :currentUserId
              AND created_at < :cursor
            ORDER BY created_at DESC
            LIMIT :limit
            """)
    Flux<Post> findCandidatesForFeed(
            @Param("currentUserId") UUID     currentUserId,
            @Param("cursor")        LocalDateTime  cursor,
            @Param("limit")         int      limit);
}
