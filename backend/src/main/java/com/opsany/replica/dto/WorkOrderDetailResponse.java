package com.opsany.replica.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkOrderDetailResponse {

    private String orderNo;
    private String title;
    private String type;
    private String creatorUsername;
    private String creatorDisplayName;
    private String progress;
    private String status;
    private String priority;
    private String serviceName;
    private String description;
    private String estimatedAt;
    private String createdAt;
    private String updatedAt;
    private String processCode;
    private String currentNodeCode;
    private String currentNodeName;
    private String currentHandler;
    private String source;
    private String solution;
    private Boolean isRequesterConfirmed;
    private Boolean isMerged;
    private Boolean isSplit;
    private String mergedToOrderNo;
    private String originalOrderNo;
    private Integer reminderCount;
}
