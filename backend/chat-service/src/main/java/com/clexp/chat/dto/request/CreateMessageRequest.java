package com.clexp.chat.dto.request;

import java.util.List;
import java.util.UUID;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateMessageRequest {
    
    @NotNull(message = "Chat ID is required")
    private UUID chatId;
    
    @NotBlank(message = "Message content cannot be empty")
    private String content;
    
    private List<String> mediaUrls;
}
