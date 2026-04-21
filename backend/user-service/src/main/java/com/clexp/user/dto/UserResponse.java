package com.clexp.user.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
import com.clexp.common.dto.InterestDto;
import com.clexp.common.dto.LanguageDto;
import com.clexp.common.model.UserRole;

@Data
@Builder
public class UserResponse {
    private UUID id;
    private String username;
    private String fullName;
    private Integer age;
    private String location;
    private String bio;
    private String avatarUrl;
    private UserRole role;
    private LocalDateTime lastLoginAt;
    private Set<LanguageDto> knownLanguages;
    private Set<LanguageDto> learningLanguages;
    private Set<InterestDto> interests;
}
