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
public class WorkOrderReminder {

    private Long id;

    private String orderNo;

    private String reminderType;

    private String reminderUsername;

    private String reminderDisplayName;

    private String reminderContent;

    private LocalDateTime createdAt;
}
