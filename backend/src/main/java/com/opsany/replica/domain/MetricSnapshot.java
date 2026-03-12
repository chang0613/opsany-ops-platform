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
public class MetricSnapshot {

    private Long id;
    private Long deviceId;
    private String deviceCode;
    private String metricType;
    private String metricName;
    private String metricValue;
    private LocalDateTime collectedAt;
}
