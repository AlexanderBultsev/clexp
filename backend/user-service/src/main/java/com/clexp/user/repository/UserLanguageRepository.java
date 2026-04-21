package com.clexp.user.repository;

import java.util.UUID;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import com.clexp.user.model.UserLanguage;
import com.clexp.user.model.UserLanguageStatus;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface UserLanguageRepository extends R2dbcRepository<UserLanguage, UUID> {
    
    Flux<UserLanguage> findAllByUserId(UUID userId);

    Flux<UserLanguage> findAllByUserIdAndStatus(UUID userId, UserLanguageStatus status);

    Mono<Void> deleteAllByUserId(UUID userId);
}
