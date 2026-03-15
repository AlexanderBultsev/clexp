package com.clexp.common.model;

import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("user_languages")
public class UserLanguage {
    @Id
    private UUID id;
    private UUID userId;
    private Long languageId;
    private UserLanguageStatus status;
}
