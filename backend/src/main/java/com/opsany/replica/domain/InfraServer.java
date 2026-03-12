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
public class InfraServer {
    private Long id;
    private String serverCode;
    private String hostname;
    private String ip;
    private String model;
    private String cpuUsage;
    private String memUsage;
    private String diskUsage;
    private String cpuTemp;
    private String collectMethod;
    private String status;
    private LocalDateTime lastCollectedAt;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
