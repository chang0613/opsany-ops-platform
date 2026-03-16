package com.opsany.replica.domain;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkOrderMerge {

    private Long id;

    private String masterOrderNo;

    private String mergedOrderNo;

    private String mergeReason;

    private String operatorUsername;

    private String operatorDisplayName;

    private LocalDateTime createdAt;
}
