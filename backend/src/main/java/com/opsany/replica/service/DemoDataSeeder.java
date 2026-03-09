package com.opsany.replica.service;

import java.time.LocalDateTime;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.opsany.replica.config.AppProperties;
import com.opsany.replica.domain.AppUser;
import com.opsany.replica.domain.NotificationMessage;
import com.opsany.replica.domain.TaskRecord;
import com.opsany.replica.domain.WorkOrder;
import com.opsany.replica.repository.AppUserRepository;
import com.opsany.replica.repository.NotificationMessageRepository;
import com.opsany.replica.repository.TaskRecordRepository;
import com.opsany.replica.repository.WorkOrderRepository;
import com.opsany.replica.security.PasswordCodec;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DemoDataSeeder implements ApplicationRunner {

    private final AppProperties appProperties;
    private final PlatformTemplateService platformTemplateService;
    private final AppUserRepository appUserRepository;
    private final WorkOrderRepository workOrderRepository;
    private final TaskRecordRepository taskRecordRepository;
    private final NotificationMessageRepository notificationMessageRepository;
    private final PasswordCodec passwordCodec;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!appProperties.getSeed().isEnabled()) {
            return;
        }

        AppUser demoUser = seedUser();
        seedWorkOrders(demoUser);
        seedTasks();
        seedMessages();
    }

    private AppUser seedUser() {
        return appUserRepository.findByUsername("demo").orElseGet(() ->
            appUserRepository.save(AppUser.builder()
                .username("demo")
                .displayName("演示用户")
                .passwordHash(passwordCodec.encode("123456.coM"))
                .status("ACTIVE")
                .createdAt(LocalDateTime.now())
                .build())
        );
    }

    private void seedWorkOrders(AppUser demoUser) {
        if (workOrderRepository.count() > 0) {
            return;
        }
        ObjectNode template = platformTemplateService.copyTemplate();
        ObjectNode page = (ObjectNode) template.with("pages").get("/personSetting/orderManage");
        ArrayNode rows = page.withArray("rows");
        for (JsonNode row : rows) {
            workOrderRepository.save(WorkOrder.builder()
                .orderNo(row.path("id").asText())
                .title(row.path("title").asText())
                .type(row.path("type").asText())
                .creatorUsername(demoUser.getUsername())
                .creatorDisplayName(row.path("creator").asText("演示用户"))
                .progress(row.path("progress").asText())
                .status(row.path("status").asText())
                .priority("中")
                .serviceName(row.path("title").asText())
                .description(row.path("title").asText())
                .estimatedAt(row.path("eta").asText("--"))
                .createdAt(DateFormats.parseFlexible(row.path("createdAt").asText()))
                .build());
        }
    }

    private void seedTasks() {
        if (taskRecordRepository.count() > 0) {
            return;
        }
        ObjectNode template = platformTemplateService.copyTemplate();
        ObjectNode page = (ObjectNode) template.with("pages").get("/personSetting/taskManage");
        ArrayNode rows = page.withArray("rows");
        for (JsonNode row : rows) {
            taskRecordRepository.save(TaskRecord.builder()
                .taskNo(row.path("id").asText())
                .title(row.path("title").asText())
                .source(row.path("source").asText())
                .ticket(row.path("ticket").asText())
                .status(row.path("status").asText())
                .assignee(row.path("assignee").asText())
                .priority(row.path("priority").asText("中"))
                .creator(row.path("creator").asText())
                .createdAt(DateFormats.parseFlexible(row.path("createdAt").asText()))
                .build());
        }
    }

    private void seedMessages() {
        if (notificationMessageRepository.count() > 0) {
            return;
        }
        ObjectNode template = platformTemplateService.copyTemplate();
        ObjectNode page = (ObjectNode) template.with("pages").get("/msgCenter/messageManage");
        ArrayNode rows = page.withArray("rows");
        for (JsonNode row : rows) {
            notificationMessageRepository.save(NotificationMessage.builder()
                .title(row.path("title").asText())
                .messageType(row.path("type").asText("工作台"))
                .sentAt(DateFormats.parseFlexible(row.path("time").asText()))
                .read(false)
                .build());
        }
    }
}
