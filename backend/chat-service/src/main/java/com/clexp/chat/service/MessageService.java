package com.clexp.chat.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.clexp.chat.dto.request.CreateMessageRequest;
import com.clexp.chat.dto.request.UpdateMessageRequest;
import com.clexp.chat.dto.response.MessageResponse;
import com.clexp.chat.model.Message;
import com.clexp.chat.model.MessageStatus;
import com.clexp.chat.model.MessageStatusType;
import com.clexp.chat.repository.ChatUserRepository;
import com.clexp.chat.repository.MessageRepository;
import com.clexp.chat.repository.MessageStatusRepository;
import com.clexp.common.exception.BusinessException;
import com.clexp.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageService {
    
    private final MessageRepository messageRepository;
    private final MessageStatusRepository messageStatusRepository;
    private final ChatUserRepository chatUserRepository;
    private final UserService userService;
    private final WebSocketSessionService sessionService;
    private final WebSocketNotificationService notificationService;
    
    @Transactional
    public Mono<MessageResponse> sendMessage(UUID senderId, CreateMessageRequest request) {
        log.info("Sending message to chat {} from user {}", request.getChatId(), senderId);
        
        return chatUserRepository.existsByChatIdAndUserId(request.getChatId(), senderId)
            .flatMap(isParticipant -> {
                if (!isParticipant) {
                    return Mono.error(new BusinessException(
                        "User is not a participant of this chat", 
                        HttpStatus.FORBIDDEN
                    ));
                }
                
                Message message = Message.builder()
                    .chatId(request.getChatId())
                    .userId(senderId)
                    .content(request.getContent())
                    .mediaUrls(request.getMediaUrls())
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
                
                return messageRepository.save(message);
            })
            .flatMap(savedMessage ->
                createInitialStatuses(savedMessage.getId(), savedMessage.getChatId(), senderId)
                    .thenReturn(savedMessage)
            )
            .flatMap(this::mapToMessageResponse)
            .flatMap(messageResponse -> 
                notificationService.notifyNewMessage(messageResponse.getChatId(), messageResponse)
                    .thenReturn(messageResponse)
            );
    }
    
    public Flux<MessageResponse> getMessageHistory(UUID userId, UUID chatId, int page, int size) {
        return chatUserRepository.existsByChatIdAndUserId(chatId, userId)
            .flatMapMany(isParticipant -> {
                if (!isParticipant) {
                    return Flux.error(new BusinessException(
                        "User is not a participant of this chat", 
                        HttpStatus.FORBIDDEN
                    ));
                }
                
                int offset = page * size;
                return messageRepository
                    .findByChatIdOrderByCreatedAtDesc(chatId, size, offset)
                    .flatMap(this::mapToMessageResponse);
            });
    }
    
    @Transactional
    public Mono<MessageResponse> updateMessage(UUID userId, UUID messageId, UpdateMessageRequest request) {
        return messageRepository.findById(messageId)
            .switchIfEmpty(Mono.error(new BusinessException("Message not found", HttpStatus.NOT_FOUND)))
            .flatMap(message -> {
                if (!message.getUserId().equals(userId)) {
                    return Mono.error(new BusinessException(
                        "Only message sender can update it", 
                        HttpStatus.FORBIDDEN
                    ));
                }
                
                message.setContent(request.getContent());
                message.setMediaUrls(request.getMediaUrls());
                message.setUpdatedAt(LocalDateTime.now());
                
                return messageRepository.save(message);
            })
            .flatMap(this::mapToMessageResponse)
            .flatMap(messageResponse ->
                notificationService.notifyMessageUpdated(messageResponse.getChatId(), messageResponse)
                    .thenReturn(messageResponse)
            );
    }
    
    @Transactional
    public Mono<Void> deleteMessage(UUID userId, UUID messageId) {
        return messageRepository.findById(messageId)
            .switchIfEmpty(Mono.error(new BusinessException("Message not found", HttpStatus.NOT_FOUND)))
            .flatMap(message -> {
                if (!message.getUserId().equals(userId)) {
                    return Mono.error(new BusinessException(
                        "Only message sender can delete it", 
                        HttpStatus.FORBIDDEN
                    ));
                }

                UUID chatId = message.getChatId();
                
                return messageRepository.deleteById(messageId)
                    .then(notificationService.notifyMessageDeleted(chatId, messageId));
            });
    }
    
    @Transactional
    public Mono<Void> markAsRead(UUID userId, List<UUID> messageIds) {
        if (messageIds == null || messageIds.isEmpty()) {
            return Mono.empty();
        }
        
        return Flux.fromIterable(messageIds)
            .flatMap(messageId -> 
                messageStatusRepository.upsertStatus(messageId, userId, MessageStatusType.READ)
                    .then(messageRepository.findById(messageId))
                    .flatMap(message -> 
                        notificationService.notifyStatusChanged(
                            message.getChatId(), 
                            messageId, 
                            userId, 
                            MessageStatusType.READ
                        )
                    )
            )
            .then();
    }
    
    @Transactional
    public Mono<Void> markAsDelivered(UUID userId, List<UUID> messageIds) {
        if (messageIds == null || messageIds.isEmpty()) {
            return Mono.empty();
        }
        
        return Flux.fromIterable(messageIds)
            .flatMap(messageId -> 
                messageStatusRepository.upsertStatus(messageId, userId, MessageStatusType.DELIVERED)
                    .then(messageRepository.findById(messageId))
                    .flatMap(message -> 
                        // Уведомляем об изменении статуса
                        notificationService.notifyStatusChanged(
                            message.getChatId(), 
                            messageId, 
                            userId, 
                            MessageStatusType.DELIVERED
                        )
                    )
            )
            .then();
    }

    @Transactional
    public Mono<Void> markUndeliveredMessagesAsDelivered(UUID userId) {
        return messageStatusRepository.findAllByUserIdAndStatus(userId, MessageStatusType.SENT)
            .flatMap(status -> {
                status.setStatus(MessageStatusType.DELIVERED);
                return messageStatusRepository.save(status)
                    .flatMap(updatedStatus -> 
                        messageRepository.findById(updatedStatus.getMessageId())
                            .flatMap(message -> 
                                notificationService.notifyStatusChanged(
                                    message.getChatId(),
                                    message.getId(),
                                    userId,
                                    MessageStatusType.DELIVERED
                                )
                            )
                    );
            })
            .then();
    }
    
    /**
     * Создает начальные статусы для нового сообщения:
     * - SENT для отправителя
     * - DELIVERED для остальных участников (если они онлайн, это будет обновлено через WebSocket)
     */
     private Mono<Void> createInitialStatuses(UUID messageId, UUID chatId, UUID senderId) {
        return chatUserRepository.findAllByChatId(chatId)
            .flatMap(chatUser -> {
                MessageStatusType status;
                
                if (chatUser.getUserId().equals(senderId)) {
                    status = MessageStatusType.SENT;
                } else {
                    boolean isOnline = sessionService.isUserOnline(chatUser.getUserId());
                    status = isOnline ? MessageStatusType.DELIVERED : MessageStatusType.SENT;
                }
                
                return messageStatusRepository.save(
                    MessageStatus.builder()
                        .messageId(messageId)
                        .userId(chatUser.getUserId())
                        .status(status)
                        .build()
                );
            })
            .then();
    }
    
    private Mono<MessageResponse> mapToMessageResponse(Message message) {
        return Mono.zip(
            userService.getShortUserById(message.getUserId()),
            getMessageStatuses(message.getId())
        ).map(tuple -> MessageResponse.builder()
            .id(message.getId())
            .chatId(message.getChatId())
            .sender(tuple.getT1())
            .content(message.getContent())
            .mediaUrls(message.getMediaUrls())
            .createdAt(message.getCreatedAt())
            .updatedAt(message.getUpdatedAt())
            .statuses(tuple.getT2())
            .build()
        );
    }
    
    public Mono<Map<UUID, MessageStatusType>> getMessageStatuses(UUID messageId) {
        return messageStatusRepository.findAllByMessageId(messageId)
            .collectList()
            .map(statuses -> statuses.stream()
                .collect(Collectors.toMap(
                    MessageStatus::getUserId,
                    MessageStatus::getStatus
                ))
            );
    }
}