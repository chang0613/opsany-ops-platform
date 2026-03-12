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
public class MiddlewareInstance {
    private Long id;
    private String instanceCode;
    private String name;
    private String middlewareType;
    private String version;
    private String host;
    private Integer port;
    private String cpuUsage;
    private String memUsage;
    private String connectCount;
    private String qps;
    private String status;
    private LocalDateTime lastCollectedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
