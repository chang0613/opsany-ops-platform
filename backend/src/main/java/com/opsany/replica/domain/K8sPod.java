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
public class K8sPod {
    private Long id;
    private String podCode;
    private String clusterCode;
    private String namespace;
    private String podName;
    private String nodeName;
    private String cpuUsage;
    private String memUsage;
    private Integer restartCount;
    private String status;
    private LocalDateTime createdAt;
}
