package com.clexp.user.dto;

import java.util.UUID;
import com.clexp.user.model.UserLanguageStatus;
import lombok.Data;

@Data
public class LanguagePreference {
    private UUID languageId;
    private UserLanguageStatus status;
}