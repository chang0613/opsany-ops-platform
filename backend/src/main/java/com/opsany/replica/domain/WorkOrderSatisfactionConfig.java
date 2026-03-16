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
public class WorkOrderSatisfactionConfig {

    private Long id;

    private Integer score;

    private String scoreLabel;

    private String color;

    private Integer sortOrder;

    private Boolean enabled;
}
