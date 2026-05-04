package com.clexp.chat.dto.request;

import java.util.List;
import java.util.UUID;
import com.clexp.chat.model.ChatType;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateChatRequest {

    @NotNull(message = "Chat type is required")
    private ChatType type;

    // Для PRIVATE - должен быть ровно 1 участник (второй - текущий пользователь)
    // Для GROUP - может быть несколько
    @NotEmpty(message = "Participant list cannot be empty")
    private List<UUID> participantIds;

    // Только для GROUP чатов
    @Size(max = 255, message = "Chat name must not exceed 255 characters")
    private String name;

    private String avatarUrl;
}
