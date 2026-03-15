package com.clexp.user.repository;

import java.util.Collection;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import com.clexp.common.model.Language;

@Repository
public interface LanguageRepository extends R2dbcRepository<Language, Long> {
    
    Flux<Language> findAllByIdIn(Collection<Long> ids);
}
