package com.clexp.post.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import com.clexp.common.dto.InterestDto;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PostResponse {
    private UUID id;
    private UUID userId;
    private String content;
    private String mediaUrls;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long commentsCount;
    private Long likesCount;
    private Boolean isLiked;
    private List<InterestDto> interests;
}
