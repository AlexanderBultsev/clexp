package com.clexp.user.repository;

import java.util.Collection;
import java.util.UUID;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import com.clexp.common.model.Language;
import reactor.core.publisher.Flux;

@Repository
public interface LanguageRepository extends R2dbcRepository<Language, UUID> {
    
    Flux<Language> findAllByIdIn(Collection<UUID> ids);
}
