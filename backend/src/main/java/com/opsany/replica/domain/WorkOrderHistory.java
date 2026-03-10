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
public class WorkOrderHistory {

    private Long id;
    private String orderNo;
    private String action;
    private String fromStatus;
    private String toStatus;
    private String fromNodeCode;
    private String toNodeCode;
    private String operatorUsername;
    private String operatorDisplayName;
    private String comment;
    private LocalDateTime createdAt;
}
