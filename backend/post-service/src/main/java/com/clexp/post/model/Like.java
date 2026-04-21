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
@Table("likes")
public class Like {
    @Id
    private UUID id;
    private UUID userId;
    private UUID postId;
    private LocalDateTime createdAt;
}
