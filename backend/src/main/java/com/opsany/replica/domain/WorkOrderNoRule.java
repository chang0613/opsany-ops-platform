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
public class WorkOrderNoRule {

    private Long id;

    private String ruleName;

    private String prefix;

    private Integer sequenceStart;

    private Integer sequenceCurrent;

    private Integer sequenceLength;

    private String dateFormat;

    private Boolean enabled;

    private String description;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private String creatorUsername;
}
