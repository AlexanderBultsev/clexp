package com.clexp.user.repository;

import java.util.Collection;
import java.util.UUID;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import com.clexp.common.model.Interest;
import reactor.core.publisher.Flux;

@Repository
public interface InterestRepository extends R2dbcRepository<Interest, UUID> {

    Flux<Interest> findAllByIdIn(Collection<UUID> ids);
}
