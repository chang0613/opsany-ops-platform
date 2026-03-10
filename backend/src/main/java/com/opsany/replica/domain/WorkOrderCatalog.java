package com.opsany.replica.domain;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkOrderCatalog {

    private Long id;
    private String catalogCode;
    private String name;
    private String category;
    private String type;
    private String scope;
    private Boolean online;
    private String processCode;
    private String slaName;
    private String ownerUsername;
    private String ownerDisplayName;
    private String description;
    private Integer sortNo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
