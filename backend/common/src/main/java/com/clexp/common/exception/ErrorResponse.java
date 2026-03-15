package com.clexp.common.exception;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ErrorResponse {
    private long timestamp;
    private int status;
    private String error;
    private String message;
    private String path;
}