package com.clexp.post.dto;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CommentResponse {
    private UUID id;
    private UUID postId;
    private UUID userId;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
