package com.clexp.user.repository;

import java.util.Collection;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import com.clexp.common.model.Interest;

@Repository
public interface InterestRepository extends R2dbcRepository<Interest, Long> {

    Flux<Interest> findAllByIdIn(Collection<Long> ids);
}
