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
public class WorkOrderTransitionPermission {

    private Long id;

    private String fromStatus;

    private String toStatus;

    private String roleCode;

    private Boolean allowCreator;

    private Boolean allowHandler;

    private Boolean allowAdmin;

    private Boolean isAutoTransition;

    private LocalDateTime createdAt;
}
