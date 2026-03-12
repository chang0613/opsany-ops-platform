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
public class K8sCluster {
    private Long id;
    private String clusterCode;
    private String clusterName;
    private String version;
    private Integer nodeCount;
    private Integer podCount;
    private String cpuUsage;
    private String memUsage;
    private String status;
    private LocalDateTime lastCollectedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
