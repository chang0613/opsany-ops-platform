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
public class WorkOrderSource {

    private Long id;

    private String sourceCode;

    private String sourceName;

    private Boolean enabled;

    private Integer sortOrder;

    private String description;

    private LocalDateTime createdAt;
}
