package com.clexp.post.model;

import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("comments")
public class Comment {
    @Id
    private UUID id;
    private UUID postId;
    private UUID userId;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
