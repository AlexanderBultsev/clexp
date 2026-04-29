package com.clexp.user.dto;

import java.util.List;
import java.util.UUID;
import com.clexp.common.model.UserRole;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UserUpdateRequest {
    private String fullName;
    private Integer age;
    private String location;
    private String bio;
    private String avatarUrl;

    @NotNull(message = "Role is required")
    private UserRole role;
    private List<LanguagePreference> languages;
    private List<UUID> interests;
}
