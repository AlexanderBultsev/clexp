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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import com.clexp.auth.security.CustomUserDetails;
import com.clexp.chat.dto.request.ReadMessagesRequest;
import com.clexp.chat.dto.request.UpdateMessageRequest;
import com.clexp.chat.dto.response.MessageResponse;
import com.clexp.chat.service.MessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {
    
    private final MessageService messageService;
    
    /**
     * Получить историю сообщений чата с пагинацией
     */
    @GetMapping
    public Flux<MessageResponse> getMessageHistory(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam UUID chatId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        
        return messageService.getMessageHistory(userDetails.getUserId(), chatId, page, size);
    }
    
    /**
     * Редактировать сообщение
     */
    @PutMapping("/{messageId}")
    public Mono<MessageResponse> updateMessage(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID messageId,
            @Valid @RequestBody UpdateMessageRequest request) {
        
        return messageService.updateMessage(userDetails.getUserId(), messageId, request);
    }
    
    /**
     * Удалить сообщение
     */
    @DeleteMapping("/{messageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteMessage(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID messageId) {
        
        return messageService.deleteMessage(userDetails.getUserId(), messageId);
    }
    
    /**
     * Отметить сообщения как прочитанные
     */
    @PostMapping("/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> markAsRead(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ReadMessagesRequest request) {
        
        return messageService.markAsRead(userDetails.getUserId(), request.getMessageIds());
    }
}