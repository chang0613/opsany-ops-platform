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
public class WorkOrderSatisfaction {

    private Long id;

    private String orderNo;

    private Integer score;

    private String scoreLabel;

    private String comment;

    private String evaluatorUsername;

    private String evaluatorDisplayName;

    private LocalDateTime evaluatedAt;
}
