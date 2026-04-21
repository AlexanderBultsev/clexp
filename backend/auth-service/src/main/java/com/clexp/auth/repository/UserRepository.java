package com.clexp.auth.repository;

import java.util.UUID;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import com.clexp.common.model.User;
import reactor.core.publisher.Mono;

@Repository
public interface UserRepository extends R2dbcRepository<User, UUID> {
    
    Mono<User> findByEmail(String email);
    
    Mono<User> findByUsername(String username);
    
    Mono<Boolean> existsByEmail(String email);
    
    Mono<Boolean> existsByUsername(String username);
}
