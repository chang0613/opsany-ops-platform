package com.opsany.replica.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopologyLink {
    private Long id;
    private String topologyCode;
    private String sourceNode;
    private String targetNode;
    private String linkType;
    private String latency;
    private String errorRate;
    private String status;
}
