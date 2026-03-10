package com.opsany.replica.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opsany.replica.domain.WorkOrderProcessDefinition;
import com.opsany.replica.domain.WorkOrderProcessNode;
import com.opsany.replica.dto.ProcessDefinitionDetailResponse;
import com.opsany.replica.dto.ProcessNodePayload;
import com.opsany.replica.dto.SaveProcessDefinitionRequest;
import com.opsany.replica.repository.WorkOrderProcessRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WorkOrderProcessService {

    public static final String DEFAULT_PROCESS_CODE = "STANDARD_REQUEST";

    private final WorkOrderProcessRepository workOrderProcessRepository;
    private final ObjectMapper objectMapper;

    public List<WorkOrderProcessDefinition> listDefinitions() {
        return workOrderProcessRepository.findAllDefinitions();
    }

    public List<WorkOrderProcessNode> listNodes(String processCode) {
        return workOrderProcessRepository.findNodesByProcessCode(processCode);
    }

    public ProcessDefinitionDetailResponse getDetail(String processCode) {
        String effectiveCode = StringUtils.hasText(processCode) ? processCode : DEFAULT_PROCESS_CODE;
        return new ProcessDefinitionDetailResponse(
            workOrderProcessRepository.findByProcessCode(effectiveCode),
            workOrderProcessRepository.findNodesByProcessCode(effectiveCode)
        );
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
            .definitionJson(resolveDefinitionJson(request, existing))
            .build();

        if (existing == null) {
            workOrderProcessRepository.insertDefinition(definition);
        } else {
            workOrderProcessRepository.updateDefinition(definition);
        }

        if (request.getNodes() != null && !request.getNodes().isEmpty()) {
            workOrderProcessRepository.deleteNodesByProcessCode(processCode);
            List<ProcessNodePayload> nodes = new ArrayList<ProcessNodePayload>(request.getNodes());
            for (int index = 0; index < nodes.size(); index++) {
                ProcessNodePayload node = nodes.get(index);
                workOrderProcessRepository.insertNode(WorkOrderProcessNode.builder()
                    .processCode(processCode)
                    .nodeCode(defaultIfBlank(node.getNodeCode(), "NODE_" + (index + 1)))
                    .nodeName(defaultIfBlank(node.getNodeName(), "节点" + (index + 1)))
                    .nodeType(defaultIfBlank(node.getNodeType(), "TASK"))
                    .sortNo(node.getSortNo() == null ? index + 1 : node.getSortNo())
                    .assigneeRole(defaultIfBlank(node.getAssigneeRole(), "ENGINEER"))
                    .nextApproveNode(node.getNextApproveNode())
                    .nextRejectNode(node.getNextRejectNode())
                    .build());
            }
        }
        return workOrderProcessRepository.findByProcessCode(processCode);
    }

    private String resolveDefinitionJson(SaveProcessDefinitionRequest request, WorkOrderProcessDefinition existing) {
        if (StringUtils.hasText(request.getDefinitionJson())) {
            return request.getDefinitionJson();
        }
        if (request.getNodes() == null || request.getNodes().isEmpty()) {
            return existing == null ? "{\"nodes\":[]}" : existing.getDefinitionJson();
        }
        try {
            return objectMapper.writeValueAsString(request.getNodes());
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("流程定义序列化失败", exception);
        }
    }

    private String defaultIfBlank(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }
}
