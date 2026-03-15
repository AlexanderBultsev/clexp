package com.clexp.common.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.webflux.error.ErrorWebExceptionHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Configuration
@Order(-1)
public class GlobalErrorHandler implements ErrorWebExceptionHandler {

    private final ObjectMapper objectMapper;

    public GlobalErrorHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        ServerHttpResponse response = exchange.getResponse();
        
        if (response.isCommitted()) {
            return Mono.error(ex);
        }

        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        
        HttpStatus status;
        String message;
        String error;

        if (ex instanceof BusinessException) {
            BusinessException businessException = (BusinessException) ex;
            status = businessException.getStatus();
            message = businessException.getMessage();
            error = status.getReasonPhrase();
            log.debug("Business error: {} - {}", status, message);
            
        } else if (ex instanceof ResponseStatusException) {
            ResponseStatusException statusException = (ResponseStatusException) ex;
            status = HttpStatus.valueOf(statusException.getStatusCode().value());
            message = statusException.getReason();
            error = status.getReasonPhrase();
            log.debug("Spring error: {} - {}", status, message);
            
        } else {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
            message = "An unexpected error occurred";
            error = status.getReasonPhrase();
            log.error("Unexpected error", ex);
        }

        response.setStatusCode(status);

        Map<String, Object> errorBody = new HashMap<>();
        errorBody.put("timestamp", Instant.now().toEpochMilli());
        errorBody.put("status", status.value());
        errorBody.put("error", error);
        errorBody.put("message", message);
        errorBody.put("path", exchange.getRequest().getPath().value());

        return response.writeWith(Mono.fromSupplier(() -> {
            DataBufferFactory bufferFactory = response.bufferFactory();
            try {
                byte[] bytes = objectMapper.writeValueAsBytes(errorBody);
                return bufferFactory.wrap(bytes);
            } catch (JacksonException e) {
                log.error("Error writing error response", e);
                return bufferFactory.wrap("{\"error\":\"Internal server error\"}".getBytes());
            }
        }));
    }
}