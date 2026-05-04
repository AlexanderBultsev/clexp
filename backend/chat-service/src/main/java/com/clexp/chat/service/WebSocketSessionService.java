package com.clexp.chat.service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.socket.WebSocketSession;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

@Slf4j
@Service
public class WebSocketSessionService {

    // userId -> WebSocketSession
    private final Map<UUID, WebSocketSession> sessions = new ConcurrentHashMap<>();

    // userId -> Sink для отправки сообщений
    private final Map<UUID, Sinks.Many<String>> sinks = new ConcurrentHashMap<>();

    public void addSession(UUID userId, WebSocketSession session) {
        sessions.put(userId, session);
        Sinks.Many<String> sink = Sinks.many().multicast().onBackpressureBuffer();
        sinks.put(userId, sink);
        log.info("WebSocket session added for user: {}", userId);
    }

    public void removeSession(UUID userId) {
        sessions.remove(userId);
        Sinks.Many<String> sink = sinks.remove(userId);
        if (sink != null) {
            sink.tryEmitComplete();
        }
        log.info("WebSocket session removed for user: {}", userId);
    }

    public Mono<Void> sendMessageToUser(UUID userId, String message) {
        return Mono.fromRunnable(() -> {
            Sinks.Many<String> sink = sinks.get(userId);
            if (sink != null) {
                sink.tryEmitNext(message);
                log.debug("Message sent to user {}: {}", userId, message);
            } else {
                log.warn("No active session for user: {}", userId);
            }
        });
    }

    public Flux<String> getMessageFlux(UUID userId) {
        Sinks.Many<String> sink = sinks.get(userId);
        return sink != null ? sink.asFlux() : Flux.empty();
    }

    public boolean isUserOnline(UUID userId) {
        return sessions.containsKey(userId);
    }

    public Flux<UUID> getOnlineUsers(Flux<UUID> userIds) {
        return userIds.filter(this::isUserOnline);
    }
}
