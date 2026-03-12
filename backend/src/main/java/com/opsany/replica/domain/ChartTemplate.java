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
public class ChartTemplate {
    private Long id;
    private String templateCode;
    private String templateName;
    private String chartType;
    private String category;
    private Integer usedCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
