package com.clexp.user.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
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
    private Boolean isSubscribed;
    private LocalDateTime lastLoginAt;
    private List<LanguageDto> knownLanguages;
    private List<LanguageDto> learningLanguages;
    private List<InterestDto> interests;
}
