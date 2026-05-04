package com.clexp.chat.repository;

import java.util.UUID;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import com.clexp.chat.model.ChatUser;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface ChatUserRepository extends R2dbcRepository<ChatUser, UUID> {

    Flux<ChatUser> findAllByChatId(UUID chatId);

    Mono<ChatUser> findByChatIdAndUserId(UUID chatId, UUID userId);

    Mono<Boolean> existsByChatIdAndUserId(UUID chatId, UUID userId);

    Mono<Void> deleteByChatIdAndUserId(UUID chatId, UUID userId);

    @Query("SELECT COUNT(*) FROM chat_users WHERE chat_id = :chatId")
    Mono<Long> countByChatId(UUID chatId);
}
