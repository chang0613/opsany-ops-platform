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
public class WorkOrderExt {

    private Long id;

    private String orderNo;

    private String source;

    private String solution;

    private Boolean isRequesterConfirmed;

    private LocalDateTime requesterConfirmedAt;

    private Boolean isMerged;

    private Boolean isSplit;

    private String mergedToOrderNo;

    private String originalOrderNo;

    private String closeReason;

    private String rejectReason;

    private LocalDateTime autoCloseAt;

    private LocalDateTime lastReminderAt;

    private Integer reminderCount;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
