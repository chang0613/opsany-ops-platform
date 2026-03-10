package com.opsany.replica.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.opsany.replica.domain.WorkOrderProcessDefinition;
import com.opsany.replica.domain.WorkOrderProcessNode;
import com.opsany.replica.dto.SaveProcessDefinitionRequest;
import com.opsany.replica.repository.WorkOrderProcessRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WorkOrderProcessService {

    public static final String DEFAULT_PROCESS_CODE = "STANDARD_REQUEST";

    private final WorkOrderProcessRepository workOrderProcessRepository;

    public List<WorkOrderProcessDefinition> listDefinitions() {
        return workOrderProcessRepository.findAllDefinitions();
    }

    public List<WorkOrderProcessNode> listNodes(String processCode) {
        return workOrderProcessRepository.findNodesByProcessCode(processCode);
    }

    public WorkOrderProcessNode resolveStartNode(String processCode) {
        String effectiveCode = StringUtils.hasText(processCode) ? processCode : DEFAULT_PROCESS_CODE;
        WorkOrderProcessNode start = workOrderProcessRepository.findStartNode(effectiveCode);
        if (start == null) {
            throw new IllegalStateException("未找到工单流程起始节点: " + effectiveCode);
        }
        return start;
    }

    public WorkOrderProcessNode findNode(String processCode, String nodeCode) {
        return workOrderProcessRepository.findNode(processCode, nodeCode);
    }

    public WorkOrderProcessDefinition saveDefinition(SaveProcessDefinitionRequest request, String operator) {
        String processCode = StringUtils.hasText(request.getProcessCode()) ? request.getProcessCode() : DEFAULT_PROCESS_CODE;
        WorkOrderProcessDefinition existing = workOrderProcessRepository.findByProcessCode(processCode);
        WorkOrderProcessDefinition definition = WorkOrderProcessDefinition.builder()
            .processCode(processCode)
            .name(defaultIfBlank(request.getName(), "标准服务请求流程"))
            .category(defaultIfBlank(request.getCategory(), "请求管理"))
            .version(existing == null ? 1 : existing.getVersion() + 1)
            .status(defaultIfBlank(request.getStatus(), "已发布"))
            .owner(defaultIfBlank(operator, "管理员"))
            .creator(existing == null ? defaultIfBlank(operator, "管理员") : existing.getCreator())
            .updater(defaultIfBlank(operator, "管理员"))
            .updatedAt(LocalDateTime.now())
            .description(defaultIfBlank(request.getDescription(), "标准服务请求流程"))
            .definitionJson(request.getDefinitionJson())
            .build();

        if (existing == null) {
            workOrderProcessRepository.insertDefinition(definition);
        } else {
            workOrderProcessRepository.updateDefinition(definition);
        }
        return workOrderProcessRepository.findByProcessCode(processCode);
    }

    private String defaultIfBlank(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }
}
