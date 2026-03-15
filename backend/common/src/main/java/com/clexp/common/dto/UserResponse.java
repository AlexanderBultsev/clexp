package com.clexp.common.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import com.clexp.common.model.UserRole;

@Data
@Builder
public class UserResponse {
    private UUID id;
    private String username;
    private String firstName;
    private String lastName;
    private Integer age;
    private String location;
    private String bio;
    private String profilePictureUrl;
    private UserRole role;
    private LocalDateTime lastLoginAt;
    private Set<LanguageDto> knownLanguages;
    private Set<LanguageDto> learningLanguages;
    private Set<InterestDto> interests;
}
