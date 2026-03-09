package com.opsany.replica.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.opsany.replica.domain.NotificationMessage;
import com.opsany.replica.domain.TaskRecord;
import com.opsany.replica.messaging.WorkOrderCreatedEvent;
import com.opsany.replica.repository.NotificationMessageRepository;
import com.opsany.replica.repository.TaskRecordRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WorkOrderProjectionService {

    private final NotificationMessageRepository notificationMessageRepository;
    private final TaskRecordRepository taskRecordRepository;
    private final PlatformBootstrapService platformBootstrapService;

    @Transactional
    public void handleWorkOrderCreated(WorkOrderCreatedEvent event) {
        notificationMessageRepository.save(NotificationMessage.builder()
            .title("工单状态: " + event.getTitle())
            .messageType("工作台")
            .sentAt(LocalDateTime.now())
            .read(false)
            .build());

        taskRecordRepository.save(TaskRecord.builder()
            .taskNo(nextTaskNo())
            .title(event.getTitle() + " 工程师处理")
            .source("工单流程")
            .ticket(event.getTitle())
            .status("待处理")
            .assignee("管理员")
            .priority(event.getPriority())
            .creator(event.getCreatorDisplayName())
            .createdAt(LocalDateTime.now())
            .build());

        platformBootstrapService.evictBootstrapCacheByUsername(event.getCreatorUsername());
    }

    private String nextTaskNo() {
        return "TASK-" + DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").format(LocalDateTime.now()) + "-"
            + ThreadLocalRandom.current().nextInt(100, 999);
    }
}
