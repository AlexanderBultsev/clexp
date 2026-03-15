package com.clexp.common.dto;

import com.clexp.common.model.UserLanguageStatus;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LanguagePreference {
    private Long languageId;
    private UserLanguageStatus status;
}