package com.clexp.user.repository;

import java.util.UUID;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import com.clexp.common.model.User;

@Repository
public interface UserRepository extends R2dbcRepository<User, UUID> {
    
    Mono<User> findByEmail(String email);
    
    Mono<User> findByUsername(String username);
    
    Mono<Boolean> existsByEmail(String email);
    
    Mono<Boolean> existsByUsername(String username);
    
    @Query("SELECT * FROM users WHERE email = :email OR username = :username")
    Mono<User> findByEmailOrUsername(@Param("email") String email, @Param("username") String username);
}
