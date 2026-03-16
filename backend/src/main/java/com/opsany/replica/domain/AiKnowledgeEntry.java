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
public class AiKnowledgeEntry {

    private Long id;

    private String title;

    private String content;

    private String tags;

    private String sourceType;

    private String sourceId;

    private String ownerUsername;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}

