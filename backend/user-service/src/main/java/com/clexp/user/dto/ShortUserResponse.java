package com.clexp.user.dto;

import java.util.UUID;
import com.clexp.common.model.UserRole;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ShortUserResponse {
    private UUID id;
    private String username;
    private String avatarUrl;
    private UserRole role;
}
