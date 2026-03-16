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
public class WorkOrderStatusConfig {

    private Long id;

    private String statusCode;

    private String statusName;

    private String statusType;

    private String color;

    private Integer sortOrder;

    private Boolean isInitial;

    private Boolean isFinal;

    private Boolean enabled;

    private String description;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
