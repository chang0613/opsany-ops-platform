package com.opsany.replica.dto;

import java.util.List;

import com.opsany.replica.domain.WorkOrderProcessDefinition;
import com.opsany.replica.domain.WorkOrderProcessNode;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProcessDefinitionDetailResponse {

    private WorkOrderProcessDefinition definition;
    private List<WorkOrderProcessNode> nodes;
}
