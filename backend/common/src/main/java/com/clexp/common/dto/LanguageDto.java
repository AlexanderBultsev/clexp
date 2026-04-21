package com.clexp.common.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class LanguageDto {
    private UUID id;
    private String code;
    private String name;
}
