package com.opsany.replica.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SaveProcessDefinitionRequest {

    private String processCode;
    private String name;
    private String category;
    private String status;
    private String description;
    private String definitionJson;
    private List<ProcessNodePayload> nodes;
}
