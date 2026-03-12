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
public class AlertEvent {
    private Long id;
    private String eventCode;
    private String ruleName;
    private String source;
    private String severity;
    private String status;
    private String handler;
    private LocalDateTime triggeredAt;
    private LocalDateTime resolvedAt;
}
