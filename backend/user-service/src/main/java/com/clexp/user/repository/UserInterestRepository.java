package com.clexp.user.repository;

import java.util.UUID;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import com.clexp.common.model.UserInterest;

@Repository
public interface UserInterestRepository extends R2dbcRepository<UserInterest, UUID> {

    Flux<UserInterest> findAllByUserId(UUID userId);

    Mono<Void> deleteAllByUserId(UUID userId);
}
