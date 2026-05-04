package com.clexp.chat.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.clexp.chat.dto.request.CreateChatRequest;
import com.clexp.chat.dto.request.UpdateChatRequest;
import com.clexp.chat.dto.response.ChatResponse;
import com.clexp.chat.dto.response.MessageResponse;
import com.clexp.chat.model.Chat;
import com.clexp.chat.model.ChatType;
import com.clexp.chat.model.ChatUser;
import com.clexp.chat.repository.ChatRepository;
import com.clexp.chat.repository.ChatUserRepository;
import com.clexp.chat.repository.MessageRepository;
import com.clexp.common.exception.BusinessException;
import com.clexp.user.dto.ShortUserResponse;
import com.clexp.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {
    
    private final ChatRepository chatRepository;
    private final ChatUserRepository chatUserRepository;
    private final MessageRepository messageRepository;
    private final UserService userService;
    
    @Transactional
    public Mono<ChatResponse> createChat(UUID creatorId, CreateChatRequest request) {
        log.info("Creating chat of type {} by user {}", request.getType(), creatorId);
        
        return validateChatRequest(creatorId, request)
            .flatMap(validated -> {
                if (request.getType() == ChatType.PRIVATE) {
                    UUID otherUserId = request.getParticipantIds().get(0);
                    return chatRepository.findPrivateChatBetweenUsers(creatorId, otherUserId)
                        .flatMap(existingChat -> mapToChatResponse(existingChat, creatorId))
                        .switchIfEmpty(createNewChat(creatorId, request));
                } else {
                    return createNewChat(creatorId, request);
                }
            });
    }
    
    @Transactional
    public Mono<ChatResponse> updateChat(UUID userId, UUID chatId, UpdateChatRequest request) {
        return chatRepository.findById(chatId)
            .switchIfEmpty(Mono.error(new BusinessException("Chat not found", HttpStatus.NOT_FOUND)))
            .flatMap(chat -> {
                // Проверяем, что пользователь - создатель чата
                if (!chat.getCreatedBy().equals(userId)) {
                    return Mono.error(new BusinessException(
                        "Only chat creator can update it",
                        HttpStatus.FORBIDDEN
                    ));
                }
                
                // Проверяем, что это групповой чат
                if (chat.getType() == ChatType.PRIVATE) {
                    return Mono.error(new BusinessException(
                        "Cannot update private chat",
                        HttpStatus.BAD_REQUEST
                    ));
                }
                
                // Обновляем только заполненные поля
                if (request.getName() != null) {
                    chat.setName(request.getName());
                }
                if (request.getAvatarUrl() != null) {
                    chat.setAvatarUrl(request.getAvatarUrl());
                }
                chat.setUpdatedAt(LocalDateTime.now());
                
                return chatRepository.save(chat);
            })
            .flatMap(updatedChat -> mapToChatResponse(updatedChat, userId));
    }
    
    public Flux<ChatResponse> getUserChats(UUID userId) {
        return chatRepository.findAllByUserId(userId)
            .flatMap(chat -> mapToChatResponse(chat, userId));
    }
    
    public Mono<ChatResponse> getChatById(UUID userId, UUID chatId) {
        return chatUserRepository.existsByChatIdAndUserId(chatId, userId)
            .flatMap(isParticipant -> {
                if (!isParticipant) {
                    return Mono.error(new BusinessException(
                        "User is not a participant of this chat",
                        HttpStatus.FORBIDDEN
                    ));
                }
                
                return chatRepository.findById(chatId)
                    .switchIfEmpty(Mono.error(new BusinessException(
                        "Chat not found",
                        HttpStatus.NOT_FOUND
                    )))
                    .flatMap(chat -> mapToChatResponse(chat, userId));
            });
    }
    
    @Transactional
    public Mono<Void> leaveChat(UUID userId, UUID chatId) {
        return chatUserRepository.findByChatIdAndUserId(chatId, userId)
            .switchIfEmpty(Mono.error(new BusinessException(
                "User is not a participant of this chat",
                HttpStatus.NOT_FOUND
            )))
            .flatMap(chatUser -> chatUserRepository.delete(chatUser))
            .then();
    }
    
    @Transactional
    public Mono<Void> addParticipants(UUID chatId, List<UUID> userIds) {
        return chatRepository.findById(chatId)
            .switchIfEmpty(Mono.error(new BusinessException("Chat not found", HttpStatus.NOT_FOUND)))
            .flatMap(chat -> {
                if (chat.getType() == ChatType.PRIVATE) {
                    return Mono.error(new BusinessException(
                        "Cannot add participants to private chat",
                        HttpStatus.BAD_REQUEST
                    ));
                }

                chat.setUpdatedAt(LocalDateTime.now());
                
                return addParticipantsInternal(chatId, userIds);
            });
    }
    
    @Transactional
    public Mono<Void> deleteChat(UUID userId, UUID chatId) {
        return chatRepository.findById(chatId)
            .switchIfEmpty(Mono.error(new BusinessException("Chat not found", HttpStatus.NOT_FOUND)))
            .flatMap(chat -> {
                if (!chat.getCreatedBy().equals(userId)) {
                    return Mono.error(new BusinessException(
                        "Only chat creator can delete it",
                        HttpStatus.FORBIDDEN
                    ));
                }
                
                return chatRepository.delete(chat);
            });
    }

    // Utils
    
    private Mono<Boolean> validateChatRequest(UUID creatorId, CreateChatRequest request) {
        if (request.getType() == ChatType.PRIVATE) {
            if (request.getParticipantIds().size() != 1) {
                return Mono.error(new BusinessException(
                    "Private chat must have exactly one other participant",
                    HttpStatus.BAD_REQUEST
                ));
            }
            
            UUID otherUserId = request.getParticipantIds().get(0);
            if (otherUserId.equals(creatorId)) {
                return Mono.error(new BusinessException(
                    "Cannot create private chat with yourself",
                    HttpStatus.BAD_REQUEST
                ));
            }
        } else if (request.getType() == ChatType.GROUP) {
            if (request.getName() == null || request.getName().isBlank()) {
                return Mono.error(new BusinessException(
                    "Group chat must have a name",
                    HttpStatus.BAD_REQUEST
                ));
            }
        }
        
        return Mono.just(true);
    }

    private Mono<ChatResponse> createNewChat(UUID creatorId, CreateChatRequest request) {
        Chat chat = Chat.builder()
            .type(request.getType())
            .name(request.getName())
            .avatarUrl(request.getAvatarUrl())
            .createdBy(creatorId)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
        
        return chatRepository.save(chat)
            .flatMap(savedChat -> {
                List<UUID> allParticipants = new java.util.ArrayList<>(request.getParticipantIds());
                if (!allParticipants.contains(creatorId)) {
                    allParticipants.add(creatorId);
                }
                
                return addParticipantsInternal(savedChat.getId(), allParticipants)
                    .then(mapToChatResponse(savedChat, creatorId));
            });
    }

    private Mono<Void> addParticipantsInternal(UUID chatId, List<UUID> userIds) {
        return Flux.fromIterable(userIds)
            .flatMap(userId -> 
                chatUserRepository.existsByChatIdAndUserId(chatId, userId)
                    .flatMap(exists -> {
                        if (!exists) {
                            return chatUserRepository.save(
                                ChatUser.builder()
                                    .chatId(chatId)
                                    .userId(userId)
                                    .build()
                            );
                        }
                        return Mono.empty();
                    })
            )
            .then();
    }
    
    private Mono<ChatResponse> mapToChatResponse(Chat chat, UUID currentUserId) {
        return Mono.zip(
            getParticipants(chat.getId()),
            getLastMessage(chat.getId()),
            getUnreadCount(chat.getId(), currentUserId),
            getChatDisplayInfo(chat, currentUserId)
        ).map(tuple -> {
            ChatDisplayInfo displayInfo = tuple.getT4();
            MessageResponse lastMessage = tuple.getT2().getId() != null ? tuple.getT2() : null;
            
            return ChatResponse.builder()
                .id(chat.getId())
                .type(chat.getType())
                .name(displayInfo.name)
                .avatarUrl(displayInfo.avatarUrl)
                .participants(tuple.getT1())
                .lastMessage(lastMessage)
                .unreadCount(tuple.getT3())
                .createdAt(chat.getCreatedAt())
                .updatedAt(chat.getUpdatedAt())
                .build();
        });
    }
    
    private Mono<List<ShortUserResponse>> getParticipants(UUID chatId) {
        return chatUserRepository.findAllByChatId(chatId)
            .flatMap(chatUser -> userService.getShortUserById(chatUser.getUserId()))
            .collectList();
    }
    
    private Mono<MessageResponse> getLastMessage(UUID chatId) {
        return messageRepository.findLastMessageByChatId(chatId)
            .flatMap(message -> 
                userService.getShortUserById(message.getUserId())
                    .map(sender -> MessageResponse.builder()
                        .id(message.getId())
                        .chatId(message.getChatId())
                        .sender(sender)
                        .content(message.getContent())
                        .mediaUrls(message.getMediaUrls())
                        .createdAt(message.getCreatedAt())
                        .updatedAt(message.getUpdatedAt())
                        .build()
                    )
            ).defaultIfEmpty(
                MessageResponse.builder().build()
            );
    }
    
    private Mono<Long> getUnreadCount(UUID chatId, UUID userId) {
        return messageRepository.countUnreadMessages(chatId, userId)
            .defaultIfEmpty(0L);
    }
    
    private Mono<ChatDisplayInfo> getChatDisplayInfo(Chat chat, UUID currentUserId) {
        if (chat.getType() == ChatType.GROUP) {
            return Mono.just(new ChatDisplayInfo(chat.getName(), chat.getAvatarUrl()));
        }
        
        return chatUserRepository.findAllByChatId(chat.getId())
            .filter(chatUser -> !chatUser.getUserId().equals(currentUserId))
            .next()
            .flatMap(chatUser -> userService.getShortUserById(chatUser.getUserId()))
            .map(otherUser -> new ChatDisplayInfo(otherUser.getUsername(), otherUser.getAvatarUrl()))
            .defaultIfEmpty(new ChatDisplayInfo("Unknown", null));
    }
    
    private static class ChatDisplayInfo {
        String name;
        String avatarUrl;
        
        ChatDisplayInfo(String name, String avatarUrl) {
            this.name = name;
            this.avatarUrl = avatarUrl;
        }
    }
}