package com.clexp.auth.repository;

import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.clexp.common.model.User;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface UserRepository extends R2dbcRepository<User, UUID> {
    
    Mono<User> findByEmail(String email);
    
    Mono<User> findByUsername(String username);
    
    Mono<Boolean> existsByEmail(String email);
    
    Mono<Boolean> existsByUsername(String username);

    @Query("""
            SELECT *
            FROM users
            WHERE id != :currentUserId
              AND last_login_at < :cursor
            ORDER BY last_login_at DESC
            LIMIT :limit
            """)
    Flux<User> findCandidatesForFeed(
            @Param("currentUserId") UUID     currentUserId,
            @Param("cursor")        LocalDateTime  cursor,
            @Param("limit")         int      limit);
}
