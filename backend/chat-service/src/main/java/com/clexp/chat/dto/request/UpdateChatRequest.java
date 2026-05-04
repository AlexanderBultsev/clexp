package com.clexp.chat.dto.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateChatRequest {
    
    @Size(max = 255, message = "Chat name must not exceed 255 characters")
    private String name;
    
    private String avatarUrl;
}