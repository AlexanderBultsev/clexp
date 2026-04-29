package com.clexp.user.repository;

import java.util.UUID;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import com.clexp.user.model.Subscription;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface SubscriptionRepository extends R2dbcRepository<Subscription, UUID> {

    Mono<Boolean> existsBySourceIdAndTargetId(UUID sourceId, UUID targetId);

    Flux<Subscription> findAllBySourceId(UUID sourceId);

    Flux<Subscription> findAllByTargetId(UUID targetId);

    Mono<Void> deleteBySourceIdAndTargetId(UUID sourceId, UUID targetId);
}
