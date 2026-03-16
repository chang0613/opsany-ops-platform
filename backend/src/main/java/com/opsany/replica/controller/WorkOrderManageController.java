package com.opsany.replica.controller;

import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.opsany.replica.domain.WorkOrder;
import com.opsany.replica.domain.WorkOrderAttachment;
import com.opsany.replica.domain.WorkOrderMerge;
import com.opsany.replica.domain.WorkOrderProcessLog;
import com.opsany.replica.domain.WorkOrderReminder;
import com.opsany.replica.domain.WorkOrderResource;
import com.opsany.replica.domain.WorkOrderSatisfaction;
import com.opsany.replica.domain.WorkOrderSatisfactionConfig;
import com.opsany.replica.domain.WorkOrderSource;
import com.opsany.replica.domain.WorkOrderSplit;
import com.opsany.replica.domain.WorkOrderStatusConfig;
import com.opsany.replica.domain.WorkOrderTransitionPermission;
import com.opsany.replica.dto.CreateWorkOrderRequest;
import com.opsany.replica.dto.SatisfactionEvaluateRequest;
import com.opsany.replica.dto.WorkOrderDetailResponse;
import com.opsany.replica.dto.WorkOrderMergeRequest;
import com.opsany.replica.dto.WorkOrderReminderRequest;
import com.opsany.replica.dto.WorkOrderSplitRequest;
import com.opsany.replica.dto.WorkOrderTransitionRequest;
import com.opsany.replica.dto.UpdateWorkOrderRequest;
import com.opsany.replica.security.AuthInterceptor;
import com.opsany.replica.security.SessionUser;
import com.opsany.replica.service.WorkOrderManageService;
import com.opsany.replica.service.WorkOrderNoRuleService;
import com.opsany.replica.service.WorkOrderStatusService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/workbench/work-order")
@RequiredArgsConstructor
@Validated
public class WorkOrderManageController {

    private final WorkOrderManageService workOrderManageService;
    private final WorkOrderNoRuleService workOrderNoRuleService;
    private final WorkOrderStatusService workOrderStatusService;

    @PostMapping
    public WorkOrder createOrder(
        @Valid @RequestBody CreateWorkOrderRequest request,
        HttpServletRequest servletRequest
    ) {
        SessionUser sessionUser = (SessionUser) servletRequest.getAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE);
        return workOrderManageService.createOrder(request, sessionUser);
    }

    @GetMapping
    public List<WorkOrder> listOrders(
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String priority,
        @RequestParam(required = false) String serviceName,
        @RequestParam(required = false) String searchKeyword,
        HttpServletRequest servletRequest
    ) {
        SessionUser sessionUser = (SessionUser) servletRequest.getAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE);
        return workOrderManageService.listOrders(status, priority, serviceName, searchKeyword, sessionUser);
    }

    @GetMapping("/{orderNo}")
    public WorkOrderDetailResponse getOrderDetail(@PathVariable String orderNo) {
        return workOrderManageService.getOrderDetail(orderNo);
    }

    @PutMapping("/{orderNo}")
    public WorkOrder updateOrder(
        @PathVariable String orderNo,
        @Valid @RequestBody UpdateWorkOrderRequest request,
        HttpServletRequest servletRequest
    ) {
        SessionUser sessionUser = (SessionUser) servletRequest.getAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE);
        return workOrderManageService.updateOrder(orderNo, request, sessionUser);
    }

    @DeleteMapping("/{orderNo}")
    public void deleteOrder(
        @PathVariable String orderNo,
        HttpServletRequest servletRequest
    ) {
        SessionUser sessionUser = (SessionUser) servletRequest.getAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE);
        workOrderManageService.deleteOrder(orderNo, sessionUser);
    }

    @PostMapping("/{orderNo}/transition")
    public WorkOrder transitionOrder(
        @PathVariable String orderNo,
        @Valid @RequestBody WorkOrderTransitionRequest request,
        HttpServletRequest servletRequest
    ) {
        SessionUser sessionUser = (SessionUser) servletRequest.getAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE);
        return workOrderManageService.transitionOrder(orderNo, request, sessionUser);
    }

    @PostMapping("/{orderNo}/confirm-close")
    public WorkOrder confirmClose(
        @PathVariable String orderNo,
        HttpServletRequest servletRequest
    ) {
        SessionUser sessionUser = (SessionUser) servletRequest.getAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE);
        return workOrderManageService.confirmClose(orderNo, sessionUser);
    }

    @PostMapping("/{orderNo}/reject")
    public WorkOrder rejectOrder(
        @PathVariable String orderNo,
        @RequestParam String rejectReason,
        HttpServletRequest servletRequest
    ) {
        SessionUser sessionUser = (SessionUser) servletRequest.getAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE);
        return workOrderManageService.rejectOrder(orderNo, rejectReason, sessionUser);
    }

    @PostMapping("/{orderNo}/resubmit")
    public WorkOrder resubmitOrder(
        @PathVariable String orderNo,
        HttpServletRequest servletRequest
    ) {
        SessionUser sessionUser = (SessionUser) servletRequest.getAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE);
        return workOrderManageService.resubmitOrder(orderNo, sessionUser);
    }

    @PostMapping("/{orderNo}/revert")
    public WorkOrder revertOrder(
        @PathVariable String orderNo,
        HttpServletRequest servletRequest
    ) {
        SessionUser sessionUser = (SessionUser) servletRequest.getAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE);
        return workOrderManageService.revertOrder(orderNo, sessionUser);
    }

    @PostMapping("/{orderNo}/satisfaction")
    public WorkOrderSatisfaction evaluateSatisfaction(
        @PathVariable String orderNo,
        @Valid @RequestBody SatisfactionEvaluateRequest request,
        HttpServletRequest servletRequest
    ) {
        SessionUser sessionUser = (SessionUser) servletRequest.getAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE);
        return workOrderManageService.evaluateSatisfaction(orderNo, request, sessionUser);
    }

    @GetMapping("/{orderNo}/satisfaction")
    public WorkOrderSatisfaction getSatisfaction(@PathVariable String orderNo) {
        return workOrderManageService.getSatisfactionByOrderNo(orderNo);
    }

    @PostMapping("/{orderNo}/reminder")
    public WorkOrderReminder createReminder(
        @PathVariable String orderNo,
        @RequestBody WorkOrderReminderRequest request,
        HttpServletRequest servletRequest
    ) {
        SessionUser sessionUser = (SessionUser) servletRequest.getAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE);
        return workOrderManageService.createReminder(orderNo, request, sessionUser);
    }

    @GetMapping("/{orderNo}/reminders")
    public List<WorkOrderReminder> getReminders(@PathVariable String orderNo) {
        return workOrderManageService.getRemindersByOrderNo(orderNo);
    }

    @PostMapping("/{orderNo}/merge")
    public WorkOrder mergeOrder(
        @PathVariable String orderNo,
        @Valid @RequestBody WorkOrderMergeRequest request,
        HttpServletRequest servletRequest
    ) {
        SessionUser sessionUser = (SessionUser) servletRequest.getAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE);
        return workOrderManageService.mergeOrder(orderNo, request, sessionUser);
    }

    @GetMapping("/{orderNo}/merged")
    public List<WorkOrderMerge> getMergedOrders(@PathVariable String orderNo) {
        return workOrderManageService.getMergedOrders(orderNo);
    }

    @PostMapping("/{orderNo}/split")
    public WorkOrder splitOrder(
        @PathVariable String orderNo,
        @Valid @RequestBody WorkOrderSplitRequest request,
        HttpServletRequest servletRequest
    ) {
        SessionUser sessionUser = (SessionUser) servletRequest.getAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE);
        return workOrderManageService.splitOrder(orderNo, request, sessionUser);
    }

    @GetMapping("/{orderNo}/splits")
    public List<WorkOrderSplit> getSplitOrders(@PathVariable String orderNo) {
        return workOrderManageService.getSplitOrders(orderNo);
    }

    @PostMapping("/{orderNo}/resource")
    public WorkOrder addResource(
        @PathVariable String orderNo,
        @RequestParam String resourceType,
        @RequestParam String resourceId,
        @RequestParam String resourceName,
        @RequestParam(required = false) String resourceInfo,
        HttpServletRequest servletRequest
    ) {
        SessionUser sessionUser = (SessionUser) servletRequest.getAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE);
        return workOrderManageService.addResource(orderNo, resourceType, resourceId, resourceName, resourceInfo, sessionUser);
    }

    @GetMapping("/{orderNo}/resources")
    public List<WorkOrderResource> getResources(@PathVariable String orderNo) {
        return workOrderManageService.getResourcesByOrderNo(orderNo);
    }

    @PostMapping("/{orderNo}/process-log")
    public WorkOrderProcessLog addProcessLog(
        @PathVariable String orderNo,
        @RequestParam String logType,
        @RequestParam String content,
        HttpServletRequest servletRequest
    ) {
        SessionUser sessionUser = (SessionUser) servletRequest.getAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE);
        return workOrderManageService.addProcessLog(orderNo, logType, content, sessionUser);
    }

    @GetMapping("/{orderNo}/process-logs")
    public List<WorkOrderProcessLog> getProcessLogs(@PathVariable String orderNo) {
        return workOrderManageService.getProcessLogs(orderNo);
    }

    @PostMapping("/{orderNo}/attachment")
    public WorkOrderAttachment uploadAttachment(
        @PathVariable String orderNo,
        @RequestParam("file") MultipartFile file,
        HttpServletRequest servletRequest
    ) throws IOException {
        SessionUser sessionUser = (SessionUser) servletRequest.getAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE);
        return null;
    }

    @GetMapping("/{orderNo}/attachments")
    public List<WorkOrderAttachment> getAttachments(@PathVariable String orderNo) {
        return null;
    }

    @DeleteMapping("/attachment/{attachmentId}")
    public void deleteAttachment(@PathVariable Long attachmentId) throws IOException {
    }

    @GetMapping("/sources")
    public List<WorkOrderSource> getSources() {
        return workOrderManageService.getAllSources();
    }

    @GetMapping("/status-configs")
    public List<WorkOrderStatusConfig> getStatusConfigs() {
        return workOrderStatusService.getAllStatusConfigs();
    }

    @GetMapping("/status-configs/enabled")
    public List<WorkOrderStatusConfig> getEnabledStatusConfigs() {
        return workOrderStatusService.getEnabledStatusConfigs();
    }

    @PostMapping("/status-configs")
    public WorkOrderStatusConfig createStatusConfig(@RequestBody WorkOrderStatusConfig config) {
        return workOrderStatusService.createStatusConfig(config);
    }

    @PutMapping("/status-configs/{id}")
    public WorkOrderStatusConfig updateStatusConfig(
        @PathVariable Long id,
        @RequestBody WorkOrderStatusConfig config
    ) {
        config.setId(id);
        return workOrderStatusService.updateStatusConfig(config);
    }

    @DeleteMapping("/status-configs/{id}")
    public void deleteStatusConfig(@PathVariable Long id) {
        workOrderStatusService.deleteStatusConfig(id);
    }

    @GetMapping("/transition-permissions")
    public List<WorkOrderTransitionPermission> getTransitionPermissions() {
        return workOrderStatusService.getTransitionPermissions();
    }

    @PostMapping("/transition-permissions")
    public WorkOrderTransitionPermission createTransitionPermission(@RequestBody WorkOrderTransitionPermission permission) {
        return workOrderStatusService.createTransitionPermission(permission);
    }

    @DeleteMapping("/transition-permissions/{id}")
    public void deleteTransitionPermission(@PathVariable Long id) {
        workOrderStatusService.deleteTransitionPermission(id);
    }

    @GetMapping("/no-rules")
    public List<?> getNoRules() {
        return workOrderNoRuleService.getAllRules();
    }

    @PostMapping("/no-rules")
    public Object createNoRule(@RequestBody Object request) {
        return null;
    }

    @GetMapping("/satisfaction-configs")
    public List<WorkOrderSatisfactionConfig> getSatisfactionConfigs() {
        return null;
    }
}
