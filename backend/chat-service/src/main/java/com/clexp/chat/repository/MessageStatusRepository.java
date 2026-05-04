package com.clexp.chat.repository;

import java.util.UUID;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import com.clexp.chat.model.MessageStatus;
import com.clexp.chat.model.MessageStatusType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface MessageStatusRepository extends R2dbcRepository<MessageStatus, UUID> {
    
    Flux<MessageStatus> findAllByMessageId(UUID messageId);

    Mono<MessageStatus> findByMessageIdAndUserId(UUID messageId, UUID userId);

    Mono<Boolean> existsByMessageIdAndUserId(UUID messageId, UUID userId);

    @Query("""
        INSERT INTO message_statuses (message_id, user_id, status)
        VALUES (:messageId, :userId, :#{#status.name()})
        ON CONFLICT (message_id, user_id) 
        DO UPDATE SET status = :#{#status.name()}
        """)
    @Modifying
    Mono<Void> upsertStatus(UUID messageId, UUID userId, MessageStatusType status);

    @Query("""
        SELECT ms.* FROM message_statuses ms
        WHERE ms.message_id IN (:messageIds)
        AND ms.user_id = :userId
        """)
    Flux<MessageStatus> findAllByMessageIdsAndUserId(Iterable<UUID> messageIds, UUID userId);

    @Query("""
        SELECT ms.* FROM message_statuses ms
        WHERE ms.user_id = :userId
        AND ms.status = :#{#status.name()}
        """)
    Flux<MessageStatus> findAllByUserIdAndStatus(UUID userId, MessageStatusType status);
}
