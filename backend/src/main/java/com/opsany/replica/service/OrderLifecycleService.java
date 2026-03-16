package com.opsany.replica.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import com.opsany.replica.config.AppProperties;
import com.opsany.replica.domain.WorkOrder;
import com.opsany.replica.domain.WorkOrderCatalog;
import com.opsany.replica.domain.WorkOrderHistory;
import com.opsany.replica.domain.WorkOrderProcessNode;
import com.opsany.replica.dto.CreateWorkOrderRequest;
import com.opsany.replica.dto.TransitionWorkOrderRequest;
import com.opsany.replica.dto.WorkOrderDetailResponse;
import com.opsany.replica.messaging.WorkOrderCreatedEvent;
import com.opsany.replica.repository.WorkOrderHistoryRepository;
import com.opsany.replica.repository.WorkOrderRepository;
import com.opsany.replica.security.SessionUser;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderLifecycleService {

    private final WorkOrderRepository workOrderRepository;
    private final WorkOrderHistoryRepository workOrderHistoryRepository;
    private final WorkOrderProcessService workOrderProcessService;
    private final WorkOrderCatalogService workOrderCatalogService;
    private final WorkOrderEventPublisher workOrderEventPublisher;
    private final WorkOrderProjectionService workOrderProjectionService;
    private final PlatformBootstrapService platformBootstrapService;
    private final AppProperties appProperties;

    @Transactional
    public WorkOrder createOrder(CreateWorkOrderRequest request, SessionUser sessionUser) {
        LocalDateTime now = LocalDateTime.now();
        WorkOrderCatalog catalog = resolveCatalog(request);
        String processCode = resolveProcessCode(request, catalog);
        WorkOrderProcessNode startNode = workOrderProcessService.resolveStartNode(processCode);
        WorkOrderProcessNode activeNode = resolveActiveStartNode(startNode);

        WorkOrder workOrder = WorkOrder.builder()
            .orderNo(nextOrderNo(now))
            .title(request.getTitle())
            .type(resolveOrderType(request, catalog))
            .creatorUsername(sessionUser.getUsername())
            .creatorDisplayName(sessionUser.getDisplayName())
            .progress(activeNode.getNodeName())
            .status(statusForNode(activeNode.getNodeCode()))
            .priority(defaultIfBlank(request.getPriority(), "中"))
            .serviceName(resolveServiceName(request, catalog))
            .description(defaultIfBlank(request.getDescription(), "通过 Vue3 + Spring Boot 重建的模拟工单"))
            .estimatedAt("--")
            .createdAt(now)
            .updatedAt(now)
            .processCode(activeNode.getProcessCode())
            .currentNodeCode(activeNode.getNodeCode())
            .currentNodeName(activeNode.getNodeName())
            .currentHandler(resolveHandler(activeNode.getAssigneeRole(), sessionUser.getDisplayName()))
            .build();
        workOrderRepository.insert(workOrder);

        appendHistory(workOrder, "SUBMIT", null, activeNode.getNodeCode(), sessionUser, "提交工单");

        WorkOrderCreatedEvent event = new WorkOrderCreatedEvent(
            workOrder.getId(),
            workOrder.getOrderNo(),
            workOrder.getTitle(),
            workOrder.getType(),
            workOrder.getCreatorUsername(),
            workOrder.getCreatorDisplayName(),
            workOrder.getPriority(),
            workOrder.getCreatedAt()
        );

        try {
            workOrderEventPublisher.publish(event);
        } catch (Exception exception) {
            if (appProperties.getMessaging().isFallbackOnPublishFailure()) {
                workOrderProjectionService.handleWorkOrderCreated(event);
            } else {
                throw exception;
            }
        }

        platformBootstrapService.evictBootstrapCache(sessionUser.getUserId());
        return workOrder;
    }

    public List<WorkOrder> listRecentOrders(SessionUser sessionUser) {
        return isAdmin(sessionUser)
            ? workOrderRepository.findTop20OrderByCreatedAtDesc()
            : workOrderRepository.findTop10ByCreatorUsernameOrderByCreatedAtDesc(sessionUser.getUsername());
    }

    public WorkOrderDetailResponse getOrderDetail(String orderNo) {
        WorkOrder order = workOrderRepository.findByOrderNo(orderNo);
        if (order == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "工单不存在");
        }
        List<WorkOrderHistory> histories = workOrderHistoryRepository.findByOrderNo(orderNo);
        return WorkOrderDetailResponse.builder()
            .orderNo(order.getOrderNo())
            .title(order.getTitle())
            .type(order.getType())
            .creatorUsername(order.getCreatorUsername())
            .creatorDisplayName(order.getCreatorDisplayName())
            .progress(order.getProgress())
            .status(order.getStatus())
            .priority(order.getPriority())
            .serviceName(order.getServiceName())
            .description(order.getDescription())
            .estimatedAt(order.getEstimatedAt())
            .createdAt(order.getCreatedAt() != null ? order.getCreatedAt().toString() : null)
            .updatedAt(order.getUpdatedAt() != null ? order.getUpdatedAt().toString() : null)
            .processCode(order.getProcessCode())
            .currentNodeCode(order.getCurrentNodeCode())
            .currentNodeName(order.getCurrentNodeName())
            .currentHandler(order.getCurrentHandler())
            .build();
    }

    @Transactional
    public WorkOrder transitionOrder(String orderNo, TransitionWorkOrderRequest request, SessionUser sessionUser) {
        WorkOrder order = workOrderRepository.findByOrderNo(orderNo);
        if (order == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "工单不存在");
        }
        assertCanOperate(order, sessionUser);

        WorkOrderProcessNode currentNode = workOrderProcessService.findNode(order.getProcessCode(), order.getCurrentNodeCode());
        if (currentNode == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "当前工单节点不存在");
        }

        String nextNodeCode = resolveNextNodeCode(currentNode, request.getAction());
        WorkOrderProcessNode nextNode = workOrderProcessService.findNode(order.getProcessCode(), nextNodeCode);
        if (nextNode == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "未找到下一流程节点");
        }

        order.setCurrentNodeCode(nextNode.getNodeCode());
        order.setCurrentNodeName(nextNode.getNodeName());
        order.setCurrentHandler(resolveHandler(nextNode.getAssigneeRole(), order.getCreatorDisplayName()));
        order.setStatus(statusForNode(nextNode.getNodeCode()));
        order.setProgress(nextNode.getNodeName());
        order.setUpdatedAt(LocalDateTime.now());
        if ("END_DONE".equals(nextNode.getNodeCode()) || "END_REJECTED".equals(nextNode.getNodeCode())) {
            order.setCurrentHandler(order.getCreatorDisplayName());
        }
        workOrderRepository.updateFlowState(order);

        appendHistory(order, normalizeAction(request.getAction()), currentNode.getNodeCode(), nextNode.getNodeCode(), sessionUser, request.getComment());
        workOrderProjectionService.syncTransitionArtifacts(order, normalizeAction(request.getAction()), sessionUser, request.getComment());
        platformBootstrapService.evictBootstrapCache(sessionUser.getUserId());
        return order;
    }

    private void appendHistory(
        WorkOrder workOrder,
        String action,
        String fromNodeCode,
        String toNodeCode,
        SessionUser sessionUser,
        String comment
    ) {
        workOrderHistoryRepository.insert(WorkOrderHistory.builder()
            .orderNo(workOrder.getOrderNo())
            .action(action)
            .fromStatus(fromNodeCode == null ? null : statusForNode(fromNodeCode))
            .toStatus(statusForNode(toNodeCode))
            .fromNodeCode(fromNodeCode)
            .toNodeCode(toNodeCode)
            .operatorUsername(sessionUser.getUsername())
            .operatorDisplayName(sessionUser.getDisplayName())
            .comment(comment)
            .createdAt(LocalDateTime.now())
            .build());
    }

    private WorkOrderProcessNode resolveActiveStartNode(WorkOrderProcessNode startNode) {
        if (StringUtils.hasText(startNode.getNextApproveNode())) {
            WorkOrderProcessNode activeNode = workOrderProcessService.findNode(startNode.getProcessCode(), startNode.getNextApproveNode());
            if (activeNode != null) {
                return activeNode;
            }
        }
        return startNode;
    }

    private void assertCanOperate(WorkOrder order, SessionUser sessionUser) {
        if (isAdmin(sessionUser)) {
            return;
        }
        boolean isCreator = sessionUser.getUsername().equals(order.getCreatorUsername());
        boolean isHandler = sessionUser.getDisplayName().equals(order.getCurrentHandler());
        if (!isCreator && !isHandler) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "当前用户没有处理该工单的权限");
        }
    }

    private String resolveNextNodeCode(WorkOrderProcessNode currentNode, String action) {
        if (isNegativeAction(action) && StringUtils.hasText(currentNode.getNextRejectNode())) {
            return currentNode.getNextRejectNode();
        }
        if (StringUtils.hasText(currentNode.getNextApproveNode())) {
            return currentNode.getNextApproveNode();
        }
        return currentNode.getNodeCode();
    }

    private boolean isNegativeAction(String action) {
        String normalized = normalizeAction(action);
        return "REJECT".equals(normalized) || "REOPEN".equals(normalized);
    }

    private String normalizeAction(String action) {
        if (!StringUtils.hasText(action)) {
            return "APPROVE";
        }

        String normalized = action.trim().toUpperCase();
        if ("驳回".equals(action) || "REJECT".equals(normalized)) {
            return "REJECT";
        }
        if ("重新处理".equals(action) || "REOPEN".equals(normalized)) {
            return "REOPEN";
        }
        if ("受理".equals(action) || "通过".equals(action) || "完成处理".equals(action) || "APPROVE".equals(normalized)
            || "RESOLVE".equals(normalized) || "CONFIRM".equals(normalized)) {
            return "APPROVE";
        }
        return normalized;
    }

    private String statusForNode(String nodeCode) {
        if ("TRIAGE".equals(nodeCode)) {
            return "待受理";
        }
        if ("ENGINEER_HANDLE".equals(nodeCode)) {
            return "处理中";
        }
        if ("REQUESTER_CONFIRM".equals(nodeCode)) {
            return "待确认";
        }
        if ("END_DONE".equals(nodeCode)) {
            return "已完成";
        }
        if ("END_REJECTED".equals(nodeCode)) {
            return "已驳回";
        }
        return "处理中";
    }

    private String resolveHandler(String assigneeRole, String creatorDisplayName) {
        if ("REQUESTER".equals(assigneeRole)) {
            return creatorDisplayName;
        }
        if ("END".equals(assigneeRole)) {
            return creatorDisplayName;
        }
        return "管理员";
    }

    private WorkOrderCatalog resolveCatalog(CreateWorkOrderRequest request) {
        if (StringUtils.hasText(request.getCatalogCode())) {
            WorkOrderCatalog catalog = workOrderCatalogService.findByCatalogCode(request.getCatalogCode());
            if (catalog != null) {
                return catalog;
            }
        }
        if (StringUtils.hasText(request.getServiceName())) {
            return workOrderCatalogService.findByName(request.getServiceName());
        }
        return null;
    }

    private String resolveProcessCode(CreateWorkOrderRequest request, WorkOrderCatalog catalog) {
        if (StringUtils.hasText(request.getProcessCode())) {
            return request.getProcessCode();
        }
        if (catalog != null && StringUtils.hasText(catalog.getProcessCode())) {
            return catalog.getProcessCode();
        }
        return WorkOrderProcessService.DEFAULT_PROCESS_CODE;
    }

    private String resolveOrderType(CreateWorkOrderRequest request, WorkOrderCatalog catalog) {
        if (StringUtils.hasText(request.getType())) {
            return request.getType();
        }
        return catalog == null ? "请求管理" : defaultIfBlank(catalog.getType(), "请求管理");
    }

    private String resolveServiceName(CreateWorkOrderRequest request, WorkOrderCatalog catalog) {
        if (catalog != null && StringUtils.hasText(catalog.getName())) {
            return catalog.getName();
        }
        return defaultIfBlank(request.getServiceName(), request.getTitle());
    }

    private String nextOrderNo(LocalDateTime now) {
        return now.format(DateFormats.SECOND_PRECISION).replace("-", "").replace(" ", "").replace(":", "")
            + ThreadLocalRandom.current().nextInt(10, 99);
    }

    private String defaultIfBlank(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value : defaultValue;
    }

    private boolean isAdmin(SessionUser sessionUser) {
        return sessionUser.getRoleCodes() != null && sessionUser.getRoleCodes().contains("PLATFORM_ADMIN");
    }
}
