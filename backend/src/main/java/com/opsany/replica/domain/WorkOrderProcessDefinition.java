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
public class WorkOrderProcessDefinition {

    private Long id;
    private String processCode;
    private String name;
    private String category;
    private Integer version;
    private String status;
    private String owner;
    private String creator;
    private String updater;
    private LocalDateTime updatedAt;
    private String description;
    private String definitionJson;
}
