package com.clexp.chat.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.clexp.chat.model.MessageStatusType;
import com.clexp.user.dto.ShortUserResponse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageResponse {
    
    private UUID id;
    private UUID chatId;
    private ShortUserResponse sender;
    private String content;
    private List<String> mediaUrls;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Map<userId, status> - статусы по каждому участнику
    private Map<UUID, MessageStatusType> statuses;
}
