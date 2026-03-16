package com.opsany.replica.domain;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiConversation {

    private Long id;

    private String title;

    private String ownerUsername;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}

