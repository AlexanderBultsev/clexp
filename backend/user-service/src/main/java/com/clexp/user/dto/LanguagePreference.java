package com.clexp.user.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;
import com.clexp.user.model.UserLanguageStatus;

@Data
@Builder
public class LanguagePreference {
    private UUID languageId;
    private UserLanguageStatus status;
}