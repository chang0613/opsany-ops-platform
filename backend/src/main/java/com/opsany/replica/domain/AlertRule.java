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
public class AlertRule {

    private Long id;
    private String ruleCode;
    private String name;
    private String metricType;
    private String conditionExpr;
    private String threshold;
    private String severity;
    private Boolean enabled;
    private String notifyGroup;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
