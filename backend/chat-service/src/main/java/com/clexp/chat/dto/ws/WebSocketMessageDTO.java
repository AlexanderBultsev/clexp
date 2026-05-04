package com.clexp.chat.dto.ws;


import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WebSocketMessageDTO<T> {
    
    private WebSocketMessageType type;
    private UUID chatId;
    private T payload;
    private String error;
    private Long timestamp;
    
    public static <T> WebSocketMessageDTO<T> of(WebSocketMessageType type, UUID chatId, T payload) {
        return WebSocketMessageDTO.<T>builder()
            .type(type)
            .chatId(chatId)
            .payload(payload)
            .timestamp(System.currentTimeMillis())
            .build();
    }
    
    public static <T> WebSocketMessageDTO<T> error(String error) {
        return WebSocketMessageDTO.<T>builder()
            .type(WebSocketMessageType.ERROR)
            .error(error)
            .timestamp(System.currentTimeMillis())
            .build();
    }
}
