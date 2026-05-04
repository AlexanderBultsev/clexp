package com.clexp.chat.service;

import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import com.clexp.chat.dto.response.MessageResponse;
import com.clexp.chat.dto.ws.WebSocketMessageDTO;
import com.clexp.chat.dto.ws.WebSocketMessageType;
import com.clexp.chat.model.MessageStatusType;
import com.clexp.chat.repository.ChatUserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketNotificationService {

    private final ChatUserRepository chatUserRepository;
    private final WebSocketSessionService sessionService;
    private final ObjectMapper objectMapper;

    public Mono<Void> notifyNewMessage(UUID chatId, MessageResponse message) {
        return broadcastToChat(chatId, WebSocketMessageType.NEW_MESSAGE, message);
    }

    public Mono<Void> notifyMessageUpdated(UUID chatId, MessageResponse message) {
        return broadcastToChat(chatId, WebSocketMessageType.MESSAGE_UPDATED, message);
    }

    public Mono<Void> notifyMessageDeleted(UUID chatId, UUID messageId) {
        var payload = Map.of("messageId", messageId);
        return broadcastToChat(chatId, WebSocketMessageType.MESSAGE_DELETED, payload);
    }

    public Mono<Void> notifyStatusChanged(UUID chatId, UUID messageId, UUID userId, MessageStatusType status) {
        var payload = Map.of(
            "messageId", messageId,
            "userId", userId,
            "status", status
        );
        return broadcastToChat(chatId, WebSocketMessageType.STATUS_UPDATED, payload);
    }

    private <T> Mono<Void> broadcastToChat(UUID chatId, WebSocketMessageType type, T payload) {
        return chatUserRepository.findAllByChatId(chatId)
            .flatMap(chatUser -> {
                try {
                    WebSocketMessageDTO<T> wsMessage = WebSocketMessageDTO.of(type, chatId, payload);
                    String json = objectMapper.writeValueAsString(wsMessage);
                    return sessionService.sendMessageToUser(chatUser.getUserId(), json);
                } catch (Exception e) {
                    log.error("Error broadcasting to chat participant {}", chatUser.getUserId(), e);
                    return Mono.empty();
                }
            })
            .then();
    }
}
