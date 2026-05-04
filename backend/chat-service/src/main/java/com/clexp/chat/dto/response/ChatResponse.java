package com.clexp.chat.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import com.clexp.chat.model.ChatType;
import com.clexp.user.dto.ShortUserResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {

    private UUID id;
    private ChatType type;
    private String name;
    private String avatarUrl;
    private List<ShortUserResponse> participants;
    private MessageResponse lastMessage;
    private Long unreadCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
