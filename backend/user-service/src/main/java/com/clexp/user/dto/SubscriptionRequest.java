package com.clexp.user.dto;

import java.util.UUID;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SubscriptionRequest {
    private UUID targetId;
}
