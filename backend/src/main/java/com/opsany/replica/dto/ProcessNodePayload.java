package com.opsany.replica.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProcessNodePayload {

    private String nodeCode;
    private String nodeName;
    private String nodeType;
    private Integer sortNo;
    private String assigneeRole;
    private String nextApproveNode;
    private String nextRejectNode;
}
