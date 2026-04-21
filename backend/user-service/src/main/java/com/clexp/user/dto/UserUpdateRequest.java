package com.clexp.user.dto;

import java.util.Set;
import java.util.UUID;
import lombok.Data;

@Data
public class UserUpdateRequest {
    private String location;
    private String bio;
    private String AvatarUrl;
    private Set<LanguagePreference> languages;
    private Set<UUID> interestIds;
}
