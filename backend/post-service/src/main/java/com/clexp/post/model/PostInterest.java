package com.clexp.post.model;

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
@Table("post_interests")
public class PostInterest {
    @Id
    private UUID id;
    private UUID postId;
    private UUID interestId;
}
