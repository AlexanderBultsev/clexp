package com.clexp.chat.dto.request;

import java.util.List;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateMessageRequest {
    
    @NotBlank(message = "Message content cannot be empty")
    private String content;
    
    private List<String> mediaUrls;
}
