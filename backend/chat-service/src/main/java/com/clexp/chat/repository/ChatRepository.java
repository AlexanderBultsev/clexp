package com.clexp.chat.repository;

import java.util.UUID;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import com.clexp.chat.model.Chat;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface ChatRepository extends R2dbcRepository<Chat, UUID> {
    
    @Query("""
        SELECT DISTINCT c.* FROM chats c
        JOIN chat_users cu ON c.id = cu.chat_id
        WHERE cu.user_id = :userId
        ORDER BY c.updated_at DESC
        """)
    Flux<Chat> findAllByUserId(UUID userId);

    @Query("""
        SELECT c.* FROM chats c
        WHERE c.type = 'PRIVATE'
        AND EXISTS (
            SELECT 1 FROM chat_users cu1
            WHERE cu1.chat_id = c.id AND cu1.user_id = :userId1
        )
        AND EXISTS (
            SELECT 1 FROM chat_users cu2
            WHERE cu2.chat_id = c.id AND cu2.user_id = :userId2
        )
        AND (
            SELECT COUNT(*) FROM chat_users cu3
            WHERE cu3.chat_id = c.id
        ) = 2
        LIMIT 1
        """)
    Mono<Chat> findPrivateChatBetweenUsers(UUID userId1, UUID userId2);
}
