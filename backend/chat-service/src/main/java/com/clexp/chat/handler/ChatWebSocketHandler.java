package com.clexp.chat.handler;

import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import com.clexp.auth.security.CustomUserDetails;
import com.clexp.chat.dto.request.CreateMessageRequest;
import com.clexp.chat.dto.ws.WebSocketMessageDTO;
import com.clexp.chat.dto.ws.WebSocketMessageType;
import com.clexp.chat.dto.ws.WebSocketSendMessagePayload;
import com.clexp.chat.dto.ws.WebSocketStatusPayload;
import com.clexp.chat.service.MessageService;
import com.clexp.chat.service.WebSocketSessionService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatWebSocketHandler implements WebSocketHandler {

    private final WebSocketSessionService sessionService;
    private final MessageService messageService;
    private final ObjectMapper objectMapper;

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        return ReactiveSecurityContextHolder.getContext()
            .map(securityContext -> securityContext.getAuthentication())
            .filter(Authentication::isAuthenticated)
            .map(auth -> (CustomUserDetails) auth.getPrincipal())
            .flatMap(userDetails -> {
                UUID userId = userDetails.getUserId();
                log.info("WebSocket connection established for user: {}", userId);

                // Регистрируем сессию
                sessionService.addSession(userId, session);

                // Отмечаем неполученные сообщения как DELIVERED
                messageService.markUndeliveredMessagesAsDelivered(userId)
                    .doOnSuccess(v -> log.debug("Marked undelivered messages as delivered for user {}", userId))
                    .doOnError(e -> log.error("Error marking messages as delivered for user {}", userId, e))
                    .subscribe();

                // Обрабатываем входящие сообщения от клиента
                Mono<Void> input = session.receive()
                    .flatMap(message -> handleIncomingMessage(userId, message))
                    .doOnError(error -> log.error("Error handling incoming message", error))
                    .then();
                
                // Отправляем сообщения клиенту
                Mono<Void> output = session.send(
                    sessionService.getMessageFlux(userId)
                        .map(session::textMessage)
                    )
                    .doOnError(error -> log.error("Error sending message", error));
                
                // Объединяем оба потока и очищаем при завершении
                return Mono.zip(input, output)
                    .doFinally(signalType -> {
                        sessionService.removeSession(userId);
                        log.info("WebSocket connection closed for user: {}", userId);
                    })
                    .then();
            })
            .switchIfEmpty(Mono.defer(() -> {
                log.warn("Unauthenticated WebSocket connection attempt");
                return session.close();
            }));
    }

    private Mono<Void> handleIncomingMessage(UUID userId, WebSocketMessage message) {
        try {
            String payload = message.getPayloadAsText();
            JsonNode jsonNode = objectMapper.readTree(payload);

            String typeStr = jsonNode.get("type").asText();
            WebSocketMessageType type = WebSocketMessageType.valueOf(typeStr);

            log.debug("Received WebSocket message from user {}: type={}", userId, type);

            return switch (type) {
                case SEND_MESSAGE -> handleSendMessage(userId, jsonNode);
                case MARK_AS_READ -> handleMarkAsRead(userId, jsonNode);
                case MARK_AS_DELIVERED -> handleMarkAsDelivered(userId, jsonNode);
                default -> {
                    log.warn("Unknown message type: {}", type);
                    yield Mono.empty();
                }
            };
        } catch (Exception e) {
            log.error("Error parsing WebSocket message", e);
            return sendErrorToUser(userId, "Invalid message format");
        }
    }

    private Mono<Void> handleSendMessage(UUID userId, JsonNode jsonNode) {
        try {
            WebSocketSendMessagePayload payload = objectMapper.treeToValue(
                jsonNode.get("payload"),
                WebSocketSendMessagePayload.class
            );
            
            return messageService.sendMessage(
                userId,
                new CreateMessageRequest(
                    payload.getChatId(),
                    payload.getContent(),
                    payload.getMediaUrls()
                )
            ).then();
            
        } catch (Exception e) {
            log.error("Error handling SEND_MESSAGE", e);
            return sendErrorToUser(userId, "Failed to send message");
        }
    }

    private Mono<Void> handleMarkAsRead(UUID userId, JsonNode jsonNode) {
        try {
            WebSocketStatusPayload payload = objectMapper.treeToValue(
                jsonNode.get("payload"),
                WebSocketStatusPayload.class
            );

            return messageService.markAsRead(userId, payload.getMessageIds());

        } catch (Exception e) {
            log.error("Error handling MARK_AS_READ", e);
            return sendErrorToUser(userId, "Failed to mark as read");
        }
    }

    private Mono<Void> handleMarkAsDelivered(UUID userId, JsonNode jsonNode) {
        try {
            WebSocketStatusPayload payload = objectMapper.treeToValue(
                jsonNode.get("payload"),
                WebSocketStatusPayload.class
            );

            return messageService.markAsDelivered(userId, payload.getMessageIds());

        } catch (Exception e) {
            log.error("Error handling MARK_AS_DELIVERED", e);
            return sendErrorToUser(userId, "Failed to mark as delivered");
        }
    }

    private Mono<Void> sendErrorToUser(UUID userId, String errorMessage) {
        try {
            WebSocketMessageDTO<?> wsMessage = WebSocketMessageDTO.error(errorMessage);

            String json = objectMapper.writeValueAsString(wsMessage);
            return sessionService.sendMessageToUser(userId, json);

        } catch (Exception e) {
            log.error("Error sending error message", e);
            return Mono.empty();
        }
    }
}
