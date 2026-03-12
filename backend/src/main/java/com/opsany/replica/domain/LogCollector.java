package com.opsany.replica.domain;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogCollector {

    private Long id;
    private String collectorCode;
    private String name;
    private String sourceType;
    private String sourcePath;
    private String targetHost;
    private String encoding;
    private String status;
    private Long linesCollected;
    private LocalDateTime lastCollectedAt;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
