package com.clexp.common.dto;

import java.util.Set;

import lombok.Data;

@Data
public class UserUpdateRequest {
    
    private String location;
    
    private String bio;
    
    private String profilePictureUrl;
    
    private Set<LanguagePreference> languages;
    
    private Set<Long> interestIds;
}
