package com.opsany.replica.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.opsany.replica.domain.NotificationMessage;
import com.opsany.replica.domain.TaskRecord;
import com.opsany.replica.domain.WorkOrder;
import com.opsany.replica.messaging.WorkOrderCreatedEvent;
import com.opsany.replica.repository.NotificationMessageRepository;
import com.opsany.replica.repository.TaskRecordRepository;
import com.opsany.replica.security.SessionUser;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WorkOrderProjectionService {

    private final NotificationMessageRepository notificationMessageRepository;
    private final TaskRecordRepository taskRecordRepository;
    private final PlatformBootstrapService platformBootstrapService;

    @Transactional
    public void handleWorkOrderCreated(WorkOrderCreatedEvent event) {
        notificationMessageRepository.insert(NotificationMessage.builder()
            .title("工单已提交: " + event.getTitle())
            .messageType("工作台")
            .sentAt(LocalDateTime.now())
            .read(false)
            .recipientUsername(event.getCreatorUsername())
            .sourceType("WORK_ORDER")
            .sourceId(event.getOrderNo())
            .build());

        taskRecordRepository.insert(TaskRecord.builder()
            .taskNo(nextTaskNo())
            .title(event.getTitle() + " 待处理")
            .source("工单流程")
            .ticket(event.getOrderNo())
            .status("待处理")
            .assignee("管理员")
            .priority(event.getPriority())
            .creator(event.getCreatorDisplayName())
            .createdAt(LocalDateTime.now())
            .orderNo(event.getOrderNo())
            .nodeCode("TRIAGE")
            .build());

        platformBootstrapService.evictBootstrapCacheByUsername(event.getCreatorUsername());
    }

    @Transactional
    public void syncTransitionArtifacts(WorkOrder order, String action, SessionUser operator, String comment) {
        taskRecordRepository.closeOpenTasks(order.getOrderNo(), LocalDateTime.now());

        if (!"已完成".equals(order.getStatus()) && !"已驳回".equals(order.getStatus())) {
            taskRecordRepository.insert(TaskRecord.builder()
                .taskNo(nextTaskNo())
                .title(order.getTitle() + " - " + order.getCurrentNodeName())
                .source("工单流转")
                .ticket(order.getOrderNo())
                .status("待处理")
                .assignee(order.getCurrentHandler())
                .priority(order.getPriority())
                .creator(operator.getDisplayName())
                .createdAt(LocalDateTime.now())
                .orderNo(order.getOrderNo())
                .nodeCode(order.getCurrentNodeCode())
                .build());
        }

        notificationMessageRepository.insert(NotificationMessage.builder()
            .title("工单" + order.getOrderNo() + " 已执行动作: " + action)
            .messageType("工单流转")
            .sentAt(LocalDateTime.now())
            .read(false)
            .recipientUsername(order.getCreatorUsername())
            .sourceType("WORK_ORDER")
            .sourceId(order.getOrderNo())
            .build());

        if (comment != null && !comment.trim().isEmpty()) {
            notificationMessageRepository.insert(NotificationMessage.builder()
                .title("流转备注: " + comment)
                .messageType("工单备注")
                .sentAt(LocalDateTime.now())
                .read(false)
                .recipientUsername(order.getCreatorUsername())
                .sourceType("WORK_ORDER")
                .sourceId(order.getOrderNo())
                .build());
        }

        platformBootstrapService.evictBootstrapCacheByUsername(order.getCreatorUsername());
    }

    private String nextTaskNo() {
        return "TASK-" + DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").format(LocalDateTime.now()) + "-"
            + ThreadLocalRandom.current().nextInt(100, 999);
    }
}
