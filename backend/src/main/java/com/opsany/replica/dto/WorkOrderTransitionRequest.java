package com.opsany.replica.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkOrderTransitionRequest {

    private String action;
    private String toStatus;
    private String comment;
    private String solution;
    private String logType;
    private String content;
}
