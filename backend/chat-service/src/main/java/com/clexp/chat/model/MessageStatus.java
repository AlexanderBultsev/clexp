package com.clexp.chat.model;

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
@Table("message_statuses")
public class MessageStatus {
    @Id
    private UUID id;
    private UUID messageId;
    private UUID userId;
    private MessageStatusType status;
}
