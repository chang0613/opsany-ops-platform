package com.opsany.replica.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkOrderProcessNode {

    private Long id;
    private String processCode;
    private String nodeCode;
    private String nodeName;
    private String nodeType;
    private Integer sortNo;
    private String assigneeRole;
    private String nextApproveNode;
    private String nextRejectNode;
}
