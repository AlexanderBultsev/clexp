package com.clexp.chat.dto.ws;

public enum WebSocketMessageType {
    // Client -> Server
    SEND_MESSAGE,           // Отправить сообщение
    MARK_AS_READ,          // Отметить как прочитанное
    MARK_AS_DELIVERED,     // Отметить как доставленное
    
    // Server -> Client
    NEW_MESSAGE,           // Новое сообщение
    MESSAGE_UPDATED,       // Сообщение обновлено
    MESSAGE_DELETED,       // Сообщение удалено
    STATUS_UPDATED,        // Статус сообщения обновлен
    ERROR                  // Ошибка
}
