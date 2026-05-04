package com.clexp.chat.dto.request;

import java.util.List;
import java.util.UUID;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddUsersRequest {
    
    @NotEmpty(message = "User IDs cannot be empty")
    private List<UUID> userIds;
}
