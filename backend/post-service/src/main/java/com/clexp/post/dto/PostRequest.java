package com.clexp.post.dto;

import java.util.List;
import java.util.UUID;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PostRequest {
    @NotBlank
    private String content;
    private List<String> mediaUrls;
    private List<UUID> interests;
}
