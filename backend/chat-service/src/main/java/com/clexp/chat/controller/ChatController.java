package com.clexp.chat.controller;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import com.clexp.auth.security.CustomUserDetails;
import com.clexp.chat.dto.request.AddUsersRequest;
import com.clexp.chat.dto.request.CreateChatRequest;
import com.clexp.chat.dto.request.UpdateChatRequest;
import com.clexp.chat.dto.response.ChatListResponse;
import com.clexp.chat.dto.response.ChatResponse;
import com.clexp.chat.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/chats")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ChatResponse> createChat(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CreateChatRequest request) {
        
        return chatService.createChat(userDetails.getUserId(), request);
    }

    @GetMapping
    public Mono<ChatListResponse> getUserChats(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        return chatService.getUserChats(userDetails.getUserId())
            .collectList()
            .map(chats -> ChatListResponse.builder()
                .chats(chats)
                .total(chats.size())
                .build()
            );
    }

    @GetMapping("/{chatId}")
    public Mono<ChatResponse> getChatById(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID chatId) {
        
        return chatService.getChatById(userDetails.getUserId(), chatId);
    }

    @PutMapping("/{chatId}")
    public Mono<ChatResponse> updateChat(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID chatId,
            @Valid @RequestBody UpdateChatRequest request) {
        
        return chatService.updateChat(userDetails.getUserId(), chatId, request);
    }

    @PostMapping("/{chatId}/leave")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> leaveChat(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID chatId) {
        
        return chatService.leaveChat(userDetails.getUserId(), chatId);
    }

    @DeleteMapping("/{chatId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteChat(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID chatId) {
        
        return chatService.deleteChat(userDetails.getUserId(), chatId);
    }

    @PostMapping("/{chatId}/users")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> addUsers(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID chatId,
            @RequestBody AddUsersRequest request) {
        
        return chatService.addParticipants(chatId, request.getUserIds());
    }
}
