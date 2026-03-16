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
public class WorkOrderSplit {

    private Long id;

    private String sourceOrderNo;

    private String newOrderNo;

    private String splitContent;

    private String splitReason;

    private String operatorUsername;

    private String operatorDisplayName;

    private LocalDateTime createdAt;
}
