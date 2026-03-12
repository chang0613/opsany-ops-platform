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
public class TopologyItem {
    private Long id;
    private String topologyCode;
    private String name;
    private String type;
    private Integer nodeCount;
    private Integer linkCount;
    private Integer abnormal;
    private Integer autoRefresh;
    private LocalDateTime updatedAt;
    private LocalDateTime createdAt;
}
