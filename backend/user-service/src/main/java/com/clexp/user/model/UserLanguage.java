package com.clexp.user.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("user_languages")
public class UserLanguage {
    @Id
    private UUID id;
    private UUID userId;
    private UUID languageId;
    private UserLanguageStatus status;
}
