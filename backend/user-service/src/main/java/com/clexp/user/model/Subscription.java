package com.clexp.user.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("subscriptions")
public class Subscription {
    @Id
    private UUID id;
    private UUID sourceId;
    private UUID targetId;
    private LocalDate createdAt;
}
