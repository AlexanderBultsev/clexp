package com.clexp.chat.repository;

import java.util.UUID;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import com.clexp.chat.model.Message;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface MessageRepository extends R2dbcRepository<Message, UUID> {
    @Query("""
        SELECT * FROM messages 
        WHERE chat_id = :chatId 
        ORDER BY created_at DESC
        LIMIT :limit OFFSET :offset
        """)
    Flux<Message> findByChatIdOrderByCreatedAtDesc(UUID chatId, int limit, long offset);

    @Query("""
        SELECT * FROM messages 
        WHERE chat_id = :chatId 
        ORDER BY created_at DESC
        LIMIT 1
        """)
    Mono<Message> findLastMessageByChatId(UUID chatId);

    @Query("""
        SELECT COUNT(DISTINCT m.id) 
        FROM messages m
        WHERE m.chat_id = :chatId 
        AND m.user_id != :userId
        AND NOT EXISTS (
            SELECT 1 FROM message_statuses ms 
            WHERE ms.message_id = m.id 
            AND ms.user_id = :userId 
            AND ms.status = 'READ'
        )
        """)
    Mono<Long> countUnreadMessages(UUID chatId, UUID userId);
}
