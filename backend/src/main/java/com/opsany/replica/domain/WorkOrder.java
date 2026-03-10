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
public class WorkOrder {

    private Long id;

    private String orderNo;

    private String title;

    private String type;

    private String creatorUsername;

    private String creatorDisplayName;

    private String progress;

    private String status;

    private String priority;

    private String serviceName;

    private String description;

    private String estimatedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private String processCode;

    private String currentNodeCode;

    private String currentNodeName;

    private String currentHandler;
}
