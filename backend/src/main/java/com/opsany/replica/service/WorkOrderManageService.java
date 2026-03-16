package com.opsany.replica.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import com.opsany.replica.domain.WorkOrder;
import com.opsany.replica.domain.WorkOrderAttachment;
import com.opsany.replica.domain.WorkOrderExt;
import com.opsany.replica.domain.WorkOrderHistory;
import com.opsany.replica.domain.WorkOrderMerge;
import com.opsany.replica.domain.WorkOrderProcessLog;
import com.opsany.replica.domain.WorkOrderReminder;
import com.opsany.replica.domain.WorkOrderReminderConfig;
import com.opsany.replica.domain.WorkOrderResource;
import com.opsany.replica.domain.WorkOrderSatisfaction;
import com.opsany.replica.domain.WorkOrderSatisfactionConfig;
import com.opsany.replica.domain.WorkOrderSource;
import com.opsany.replica.domain.WorkOrderSplit;
import com.opsany.replica.domain.WorkOrderStatusConfig;
import com.opsany.replica.dto.CreateWorkOrderRequest;
import com.opsany.replica.dto.SatisfactionEvaluateRequest;
import com.opsany.replica.dto.WorkOrderDetailResponse;
import com.opsany.replica.dto.WorkOrderMergeRequest;
import com.opsany.replica.dto.WorkOrderReminderRequest;
import com.opsany.replica.dto.WorkOrderSplitRequest;
import com.opsany.replica.dto.WorkOrderTransitionRequest;
import com.opsany.replica.dto.UpdateWorkOrderRequest;
import com.opsany.replica.repository.WorkOrderAttachmentRepository;
import com.opsany.replica.repository.WorkOrderExtRepository;
import com.opsany.replica.repository.WorkOrderHistoryRepository;
import com.opsany.replica.repository.WorkOrderMergeRepository;
import com.opsany.replica.repository.WorkOrderProcessLogRepository;
import com.opsany.replica.repository.WorkOrderReminderConfigRepository;
import com.opsany.replica.repository.WorkOrderReminderRepository;
import com.opsany.replica.repository.WorkOrderRepository;
import com.opsany.replica.repository.WorkOrderResourceRepository;
import com.opsany.replica.repository.WorkOrderSatisfactionConfigRepository;
import com.opsany.replica.repository.WorkOrderSatisfactionRepository;
import com.opsany.replica.repository.WorkOrderSourceRepository;
import com.opsany.replica.repository.WorkOrderSplitRepository;
import com.opsany.replica.security.SessionUser;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkOrderManageService {

    private final WorkOrderRepository workOrderRepository;
    private final WorkOrderHistoryRepository workOrderHistoryRepository;
    private final WorkOrderExtRepository workOrderExtRepository;
    private final WorkOrderSourceRepository workOrderSourceRepository;
    private final WorkOrderResourceRepository workOrderResourceRepository;
    private final WorkOrderProcessLogRepository processLogRepository;
    private final WorkOrderSatisfactionRepository satisfactionRepository;
    private final WorkOrderSatisfactionConfigRepository satisfactionConfigRepository;
    private final WorkOrderReminderRepository reminderRepository;
    private final WorkOrderReminderConfigRepository reminderConfigRepository;
    private final WorkOrderMergeRepository mergeRepository;
    private final WorkOrderSplitRepository splitRepository;
    private final WorkOrderAttachmentRepository attachmentRepository;

    private final WorkOrderNoRuleService workOrderNoRuleService;
    private final WorkOrderStatusService workOrderStatusService;

    private static final String DEFAULT_SOURCE = "MANUAL";
    private static final String DEFAULT_CONFIG_KEY = "default";

    @Transactional
    public WorkOrder createOrder(CreateWorkOrderRequest request, SessionUser sessionUser) {
        LocalDateTime now = LocalDateTime.now();

        String orderNo = workOrderNoRuleService.generateOrderNo();
        String source = StringUtils.hasText(request.getSource()) ? request.getSource() : DEFAULT_SOURCE;

        WorkOrderStatusConfig initialStatus = workOrderStatusService.getInitialStatus();

        WorkOrder workOrder = WorkOrder.builder()
            .orderNo(orderNo)
            .title(request.getTitle())
            .type(request.getType())
            .creatorUsername(sessionUser.getUsername())
            .creatorDisplayName(sessionUser.getDisplayName())
            .progress(initialStatus.getStatusName())
            .status(initialStatus.getStatusCode())
            .priority(StringUtils.hasText(request.getPriority()) ? request.getPriority() : "中")
            .serviceName(StringUtils.hasText(request.getServiceName()) ? request.getServiceName() : request.getTitle())
            .description(StringUtils.hasText(request.getDescription()) ? request.getDescription() : "")
            .estimatedAt("--")
            .createdAt(now)
            .updatedAt(now)
            .processCode(StringUtils.hasText(request.getProcessCode()) ? request.getProcessCode() : "default")
            .currentNodeCode(initialStatus.getStatusCode())
            .currentNodeName(initialStatus.getStatusName())
            .currentHandler("管理员")
            .build();

        workOrderRepository.insert(workOrder);

        WorkOrderExt ext = WorkOrderExt.builder()
            .orderNo(orderNo)
            .source(source)
            .isRequesterConfirmed(false)
            .isMerged(false)
            .isSplit(false)
            .reminderCount(0)
            .createdAt(now)
            .updatedAt(now)
            .build();
        workOrderExtRepository.insert(ext);

        addHistory(orderNo, "CREATE", null, initialStatus.getStatusCode(), sessionUser, "创建工单");

        return workOrder;
    }

    @Transactional
    public WorkOrder updateOrder(String orderNo, UpdateWorkOrderRequest request, SessionUser sessionUser) {
        WorkOrder workOrder = workOrderRepository.findByOrderNo(orderNo);
        if (workOrder == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "工单不存在");
        }

        assertCanEdit(workOrder, sessionUser);

        if (StringUtils.hasText(request.getTitle())) {
            workOrder.setTitle(request.getTitle());
        }
        if (StringUtils.hasText(request.getType())) {
            workOrder.setType(request.getType());
        }
        if (StringUtils.hasText(request.getPriority())) {
            workOrder.setPriority(request.getPriority());
        }
        if (StringUtils.hasText(request.getServiceName())) {
            workOrder.setServiceName(request.getServiceName());
        }
        if (StringUtils.hasText(request.getDescription())) {
            workOrder.setDescription(request.getDescription());
        }
        workOrder.setUpdatedAt(LocalDateTime.now());

        workOrderRepository.update(workOrder);

        addHistory(orderNo, "UPDATE", workOrder.getStatus(), workOrder.getStatus(), sessionUser, "更新工单");

        return workOrder;
    }

    @Transactional
    public void deleteOrder(String orderNo, SessionUser sessionUser) {
        WorkOrder workOrder = workOrderRepository.findByOrderNo(orderNo);
        if (workOrder == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "工单不存在");
        }

        assertCanDelete(workOrder, sessionUser);

        workOrder.setStatus("DELETED");
        workOrder.setUpdatedAt(LocalDateTime.now());
        workOrderRepository.updateFlowState(workOrder);

        addHistory(orderNo, "DELETE", workOrder.getStatus(), "DELETED", sessionUser, "删除工单");
    }

    public WorkOrderDetailResponse getOrderDetail(String orderNo) {
        WorkOrder order = workOrderRepository.findByOrderNo(orderNo);
        if (order == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "工单不存在");
        }

        WorkOrderExt ext = workOrderExtRepository.findByOrderNo(orderNo);
        List<WorkOrderHistory> histories = workOrderHistoryRepository.findByOrderNo(orderNo);
        List<WorkOrderAttachment> attachments = attachmentRepository.findByOrderNo(orderNo);

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
            .source(ext != null ? ext.getSource() : null)
            .solution(ext != null ? ext.getSolution() : null)
            .isRequesterConfirmed(ext != null ? ext.getIsRequesterConfirmed() : false)
            .isMerged(ext != null ? ext.getIsMerged() : false)
            .isSplit(ext != null ? ext.getIsSplit() : false)
            .mergedToOrderNo(ext != null ? ext.getMergedToOrderNo() : null)
            .originalOrderNo(ext != null ? ext.getOriginalOrderNo() : null)
            .reminderCount(ext != null ? ext.getReminderCount() : 0)
            .build();
    }

    public List<WorkOrder> listOrders(String status, String priority, String serviceName, String searchKeyword, SessionUser sessionUser) {
        if (isAdmin(sessionUser)) {
            return workOrderRepository.findAll(null, status, priority, serviceName, searchKeyword);
        }
        return workOrderRepository.findAll(sessionUser.getUsername(), status, priority, serviceName, searchKeyword);
    }

    @Transactional
    public WorkOrder transitionOrder(String orderNo, WorkOrderTransitionRequest request, SessionUser sessionUser) {
        WorkOrder workOrder = workOrderRepository.findByOrderNo(orderNo);
        if (workOrder == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "工单不存在");
        }

        assertCanOperate(workOrder, sessionUser);

        String fromStatus = workOrder.getStatus();
        String toStatus = request.getToStatus();

        if (!workOrderStatusService.canTransition(fromStatus, toStatus, sessionUser)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权执行此状态流转");
        }

        workOrder.setStatus(toStatus);
        workOrder.setProgress(toStatus);
        workOrder.setUpdatedAt(LocalDateTime.now());
        workOrderRepository.updateFlowState(workOrder);

        if (StringUtils.hasText(request.getSolution())) {
            workOrderExtRepository.updateSolution(orderNo, request.getSolution());
        }

        addHistory(orderNo, request.getAction(), fromStatus, toStatus, sessionUser, request.getComment());

        if ("PROCESS".equals(request.getLogType()) && StringUtils.hasText(request.getContent())) {
            addProcessLog(orderNo, "PROCESS", request.getContent(), sessionUser);
        }

        return workOrder;
    }

    @Transactional
    public WorkOrder confirmClose(String orderNo, SessionUser sessionUser) {
        WorkOrder workOrder = workOrderRepository.findByOrderNo(orderNo);
        if (workOrder == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "工单不存在");
        }

        if (!sessionUser.getUsername().equals(workOrder.getCreatorUsername())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "只有创建人可以确认关闭工单");
        }

        workOrder.setStatus("CLOSED");
        workOrder.setProgress("已关闭");
        workOrder.setUpdatedAt(LocalDateTime.now());
        workOrderRepository.updateFlowState(workOrder);

        WorkOrderExt ext = workOrderExtRepository.findByOrderNo(orderNo);
        if (ext != null) {
            ext.setIsRequesterConfirmed(true);
            ext.setRequesterConfirmedAt(LocalDateTime.now());
            ext.setUpdatedAt(LocalDateTime.now());
            workOrderExtRepository.update(ext);
        }

        addHistory(orderNo, "CONFIRM_CLOSE", workOrder.getStatus(), "CLOSED", sessionUser, "提交人确认关闭");

        return workOrder;
    }

    @Transactional
    public WorkOrder rejectOrder(String orderNo, String rejectReason, SessionUser sessionUser) {
        WorkOrder workOrder = workOrderRepository.findByOrderNo(orderNo);
        if (workOrder == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "工单不存在");
        }

        assertCanReject(workOrder, sessionUser);

        String fromStatus = workOrder.getStatus();
        workOrder.setStatus("REJECTED");
        workOrder.setProgress("已驳回");
        workOrder.setUpdatedAt(LocalDateTime.now());
        workOrderRepository.updateFlowState(workOrder);

        WorkOrderExt ext = workOrderExtRepository.findByOrderNo(orderNo);
        if (ext != null) {
            ext.setRejectReason(rejectReason);
            ext.setUpdatedAt(LocalDateTime.now());
            workOrderExtRepository.update(ext);
        }

        addHistory(orderNo, "REJECT", fromStatus, "REJECTED", sessionUser, rejectReason);

        return workOrder;
    }

    @Transactional
    public WorkOrder resubmitOrder(String orderNo, SessionUser sessionUser) {
        WorkOrder workOrder = workOrderRepository.findByOrderNo(orderNo);
        if (workOrder == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "工单不存在");
        }

        if (!sessionUser.getUsername().equals(workOrder.getCreatorUsername())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "只有创建人可以重新提交工单");
        }

        if (!"REJECTED".equals(workOrder.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "只有已驳回的工单才能重新提交");
        }

        WorkOrderStatusConfig initialStatus = workOrderStatusService.getInitialStatus();

        workOrder.setStatus(initialStatus.getStatusCode());
        workOrder.setProgress(initialStatus.getStatusName());
        workOrder.setUpdatedAt(LocalDateTime.now());
        workOrderRepository.updateFlowState(workOrder);

        WorkOrderExt ext = workOrderExtRepository.findByOrderNo(orderNo);
        if (ext != null) {
            ext.setRejectReason(null);
            ext.setUpdatedAt(LocalDateTime.now());
            workOrderExtRepository.update(ext);
        }

        addHistory(orderNo, "RESUBMIT", "REJECTED", initialStatus.getStatusCode(), sessionUser, "重新提交工单");

        return workOrder;
    }

    @Transactional
    public WorkOrder revertOrder(String orderNo, SessionUser sessionUser) {
        WorkOrder workOrder = workOrderRepository.findByOrderNo(orderNo);
        if (workOrder == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "工单不存在");
        }

        if (!sessionUser.getUsername().equals(workOrder.getCreatorUsername())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "只有创建人可以撤销工单");
        }

        if ("CLOSED".equals(workOrder.getStatus()) || "ARCHIVED".equals(workOrder.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "已关闭或已归档的工单不能撤销");
        }

        WorkOrderStatusConfig initialStatus = workOrderStatusService.getInitialStatus();

        workOrder.setStatus(initialStatus.getStatusCode());
        workOrder.setProgress(initialStatus.getStatusName());
        workOrder.setUpdatedAt(LocalDateTime.now());
        workOrderRepository.updateFlowState(workOrder);

        addHistory(orderNo, "REVERT", workOrder.getStatus(), initialStatus.getStatusCode(), sessionUser, "撤销工单");

        return workOrder;
    }

    @Transactional
    public WorkOrderSatisfaction evaluateSatisfaction(String orderNo, SatisfactionEvaluateRequest request, SessionUser sessionUser) {
        WorkOrder workOrder = workOrderRepository.findByOrderNo(orderNo);
        if (workOrder == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "工单不存在");
        }

        if (!sessionUser.getUsername().equals(workOrder.getCreatorUsername())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "只有创建人可以评价工单");
        }

        if (!"CLOSED".equals(workOrder.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "只有已关闭的工单才能评价");
        }

        WorkOrderSatisfactionConfig config = satisfactionConfigRepository.findByScore(request.getScore());
        String scoreLabel = config != null ? config.getScoreLabel() : "未评分";

        WorkOrderSatisfaction existingSatisfaction = satisfactionRepository.findByOrderNo(orderNo);

        if (existingSatisfaction != null) {
            existingSatisfaction.setScore(request.getScore());
            existingSatisfaction.setScoreLabel(scoreLabel);
            existingSatisfaction.setComment(request.getComment());
            existingSatisfaction.setEvaluatorUsername(sessionUser.getUsername());
            existingSatisfaction.setEvaluatorDisplayName(sessionUser.getDisplayName());
            existingSatisfaction.setEvaluatedAt(LocalDateTime.now());
            satisfactionRepository.update(existingSatisfaction);
            return existingSatisfaction;
        }

        WorkOrderSatisfaction satisfaction = WorkOrderSatisfaction.builder()
            .orderNo(orderNo)
            .score(request.getScore())
            .scoreLabel(scoreLabel)
            .comment(request.getComment())
            .evaluatorUsername(sessionUser.getUsername())
            .evaluatorDisplayName(sessionUser.getDisplayName())
            .evaluatedAt(LocalDateTime.now())
            .build();

        satisfactionRepository.insert(satisfaction);
        return satisfaction;
    }

    @Transactional
    public WorkOrderReminder createReminder(String orderNo, WorkOrderReminderRequest request, SessionUser sessionUser) {
        WorkOrder workOrder = workOrderRepository.findByOrderNo(orderNo);
        if (workOrder == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "工单不存在");
        }

        WorkOrderReminderConfig config = reminderConfigRepository.findByConfigKey(DEFAULT_CONFIG_KEY);
        if (config == null) {
            config = createDefaultReminderConfig();
        }

        long todayCount = reminderRepository.countTodayByUsername(sessionUser.getUsername());
        if (todayCount >= config.getMaxDailyReminders()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "今日催办次数已达上限");
        }

        WorkOrderReminder lastReminder = reminderRepository.findByOrderNoOrderByCreatedAtDesc(orderNo).stream().findFirst().orElse(null);
        if (lastReminder != null) {
            long minutesSinceLastReminder = java.time.Duration.between(lastReminder.getCreatedAt(), LocalDateTime.now()).toMinutes();
            if (minutesSinceLastReminder < config.getMinIntervalMinutes()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "催办间隔时间不足，请" + (config.getMinIntervalMinutes() - minutesSinceLastReminder) + "分钟后再催办");
            }
        }

        WorkOrderReminder reminder = WorkOrderReminder.builder()
            .orderNo(orderNo)
            .reminderType("URGE")
            .reminderUsername(sessionUser.getUsername())
            .reminderDisplayName(sessionUser.getDisplayName())
            .reminderContent(request.getReminderContent())
            .createdAt(LocalDateTime.now())
            .build();

        reminderRepository.insert(reminder);

        workOrderExtRepository.updateReminderCount(orderNo);

        return reminder;
    }

    private WorkOrderReminderConfig createDefaultReminderConfig() {
        WorkOrderReminderConfig config = WorkOrderReminderConfig.builder()
            .configKey(DEFAULT_CONFIG_KEY)
            .minIntervalMinutes(60)
            .maxDailyReminders(3)
            .enabled(true)
            .description("默认催办配置")
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
        reminderConfigRepository.insert(config);
        return config;
    }

    @Transactional
    public WorkOrder mergeOrder(String sourceOrderNo, WorkOrderMergeRequest request, SessionUser sessionUser) {
        WorkOrder sourceOrder = workOrderRepository.findByOrderNo(sourceOrderNo);
        if (sourceOrder == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "源工单不存在");
        }

        WorkOrder targetOrder = workOrderRepository.findByOrderNo(request.getTargetOrderNo());
        if (targetOrder == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "目标工单不存在");
        }

        if (!isAdmin(sessionUser)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "只有管理员可以合并工单");
        }

        WorkOrderMerge merge = WorkOrderMerge.builder()
            .masterOrderNo(request.getTargetOrderNo())
            .mergedOrderNo(sourceOrderNo)
            .mergeReason(request.getMergeReason())
            .operatorUsername(sessionUser.getUsername())
            .operatorDisplayName(sessionUser.getDisplayName())
            .createdAt(LocalDateTime.now())
            .build();
        mergeRepository.insert(merge);

        WorkOrderExt sourceExt = workOrderExtRepository.findByOrderNo(sourceOrderNo);
        if (sourceExt != null) {
            sourceExt.setIsMerged(true);
            sourceExt.setMergedToOrderNo(request.getTargetOrderNo());
            sourceExt.setUpdatedAt(LocalDateTime.now());
            workOrderExtRepository.update(sourceExt);
        }

        sourceOrder.setStatus("MERGED");
        sourceOrder.setUpdatedAt(LocalDateTime.now());
        workOrderRepository.updateFlowState(sourceOrder);

        addHistory(sourceOrderNo, "MERGE", sourceOrder.getStatus(), "MERGED", sessionUser, "合并到工单: " + request.getTargetOrderNo());

        return targetOrder;
    }

    @Transactional
    public WorkOrder splitOrder(String sourceOrderNo, WorkOrderSplitRequest request, SessionUser sessionUser) {
        WorkOrder sourceOrder = workOrderRepository.findByOrderNo(sourceOrderNo);
        if (sourceOrder == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "源工单不存在");
        }

        if (!isAdmin(sessionUser)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "只有管理员可以拆分工单");
        }

        String newOrderNo = workOrderNoRuleService.generateOrderNo();

        WorkOrder newOrder = WorkOrder.builder()
            .orderNo(newOrderNo)
            .title(sourceOrder.getTitle() + " [拆分]")
            .type(sourceOrder.getType())
            .creatorUsername(sourceOrder.getCreatorUsername())
            .creatorDisplayName(sourceOrder.getCreatorDisplayName())
            .progress(sourceOrder.getProgress())
            .status(sourceOrder.getStatus())
            .priority(sourceOrder.getPriority())
            .serviceName(sourceOrder.getServiceName())
            .description(request.getSplitContent())
            .estimatedAt(sourceOrder.getEstimatedAt())
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .processCode(sourceOrder.getProcessCode())
            .currentNodeCode(sourceOrder.getCurrentNodeCode())
            .currentNodeName(sourceOrder.getCurrentNodeName())
            .currentHandler(sourceOrder.getCurrentHandler())
            .build();

        workOrderRepository.insert(newOrder);

        WorkOrderExt newExt = WorkOrderExt.builder()
            .orderNo(newOrderNo)
            .source(sourceOrder.getStatus())
            .isRequesterConfirmed(false)
            .isMerged(false)
            .isSplit(true)
            .originalOrderNo(sourceOrderNo)
            .reminderCount(0)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
        workOrderExtRepository.insert(newExt);

        WorkOrderSplit split = WorkOrderSplit.builder()
            .sourceOrderNo(sourceOrderNo)
            .newOrderNo(newOrderNo)
            .splitContent(request.getSplitContent())
            .splitReason(request.getSplitReason())
            .operatorUsername(sessionUser.getUsername())
            .operatorDisplayName(sessionUser.getDisplayName())
            .createdAt(LocalDateTime.now())
            .build();
        splitRepository.insert(split);

        WorkOrderExt sourceExt = workOrderExtRepository.findByOrderNo(sourceOrderNo);
        if (sourceExt != null) {
            sourceExt.setIsSplit(true);
            sourceExt.setUpdatedAt(LocalDateTime.now());
            workOrderExtRepository.update(sourceExt);
        }

        addHistory(sourceOrderNo, "SPLIT", sourceOrder.getStatus(), sourceOrder.getStatus(), sessionUser, "拆分工单: " + newOrderNo);

        return newOrder;
    }

    @Transactional
    public WorkOrder addResource(String orderNo, String resourceType, String resourceId, String resourceName, String resourceInfo, SessionUser sessionUser) {
        WorkOrder workOrder = workOrderRepository.findByOrderNo(orderNo);
        if (workOrder == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "工单不存在");
        }

        WorkOrderResource resource = WorkOrderResource.builder()
            .orderNo(orderNo)
            .resourceType(resourceType)
            .resourceId(resourceId)
            .resourceName(resourceName)
            .resourceInfo(resourceInfo)
            .matchMethod("MANUAL")
            .createdAt(LocalDateTime.now())
            .build();

        workOrderResourceRepository.insert(resource);

        return workOrder;
    }

    @Transactional
    public WorkOrderProcessLog addProcessLog(String orderNo, String logType, String content, SessionUser sessionUser) {
        WorkOrderProcessLog log = WorkOrderProcessLog.builder()
            .orderNo(orderNo)
            .logType(logType)
            .content(content)
            .operatorUsername(sessionUser.getUsername())
            .operatorDisplayName(sessionUser.getDisplayName())
            .createdAt(LocalDateTime.now())
            .build();

        processLogRepository.insert(log);
        return log;
    }

    public List<WorkOrderSource> getAllSources() {
        return workOrderSourceRepository.findAll();
    }

    public List<WorkOrderReminder> getRemindersByOrderNo(String orderNo) {
        return reminderRepository.findByOrderNo(orderNo);
    }

    public WorkOrderSatisfaction getSatisfactionByOrderNo(String orderNo) {
        return satisfactionRepository.findByOrderNo(orderNo);
    }

    public List<WorkOrderMerge> getMergedOrders(String orderNo) {
        return mergeRepository.findByMasterOrderNo(orderNo);
    }

    public List<WorkOrderSplit> getSplitOrders(String orderNo) {
        return splitRepository.findBySourceOrderNo(orderNo);
    }

    public List<WorkOrderResource> getResourcesByOrderNo(String orderNo) {
        return workOrderResourceRepository.findByOrderNo(orderNo);
    }

    public List<WorkOrderProcessLog> getProcessLogs(String orderNo) {
        return processLogRepository.findByOrderNo(orderNo);
    }

    private void addHistory(String orderNo, String action, String fromStatus, String toStatus, SessionUser sessionUser, String comment) {
        WorkOrderHistory history = WorkOrderHistory.builder()
            .orderNo(orderNo)
            .action(action)
            .fromStatus(fromStatus)
            .toStatus(toStatus)
            .operatorUsername(sessionUser.getUsername())
            .operatorDisplayName(sessionUser.getDisplayName())
            .comment(comment)
            .createdAt(LocalDateTime.now())
            .build();
        workOrderHistoryRepository.insert(history);
    }

    private void assertCanEdit(WorkOrder workOrder, SessionUser sessionUser) {
        if (isAdmin(sessionUser)) {
            return;
        }
        if (!sessionUser.getUsername().equals(workOrder.getCreatorUsername())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权编辑此工单");
        }
    }

    private void assertCanDelete(WorkOrder workOrder, SessionUser sessionUser) {
        if (!isAdmin(sessionUser)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "只有管理员可以删除工单");
        }
    }

    private void assertCanOperate(WorkOrder workOrder, SessionUser sessionUser) {
        if (isAdmin(sessionUser)) {
            return;
        }
        boolean isCreator = sessionUser.getUsername().equals(workOrder.getCreatorUsername());
        boolean isHandler = sessionUser.getDisplayName().equals(workOrder.getCurrentHandler());
        if (!isCreator && !isHandler) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "当前用户没有处理该工单的权限");
        }
    }

    private void assertCanReject(WorkOrder workOrder, SessionUser sessionUser) {
        if (!isAdmin(sessionUser)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "只有管理员可以驳回工单");
        }
    }

    private boolean isAdmin(SessionUser sessionUser) {
        return sessionUser.getRoleCodes() != null && sessionUser.getRoleCodes().contains("PLATFORM_ADMIN");
    }
}
