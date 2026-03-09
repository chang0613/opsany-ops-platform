package com.opsany.replica.service;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.opsany.replica.config.AppProperties;
import com.opsany.replica.domain.NotificationMessage;
import com.opsany.replica.domain.TaskRecord;
import com.opsany.replica.domain.WorkOrder;
import com.opsany.replica.repository.AppUserRepository;
import com.opsany.replica.repository.NotificationMessageRepository;
import com.opsany.replica.repository.TaskRecordRepository;
import com.opsany.replica.repository.WorkOrderRepository;
import com.opsany.replica.security.SessionUser;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PlatformBootstrapService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlatformBootstrapService.class);
    private static final String BOOTSTRAP_CACHE_KEY_PREFIX = "opsany:bootstrap:";

    private final PlatformTemplateService platformTemplateService;
    private final WorkOrderRepository workOrderRepository;
    private final TaskRecordRepository taskRecordRepository;
    private final NotificationMessageRepository notificationMessageRepository;
    private final AppUserRepository appUserRepository;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final AppProperties appProperties;

    public ObjectNode getBootstrap(SessionUser sessionUser) {
        return getCachedBootstrap(sessionUser.getUserId()).orElseGet(() -> buildAndCacheBootstrap(sessionUser));
    }

    public void evictBootstrapCache(Long userId) {
        try {
            redisTemplate.delete(cacheKey(userId));
        } catch (Exception exception) {
            LOGGER.warn("Failed to evict bootstrap cache: {}", exception.getMessage());
        }
    }

    public void evictBootstrapCacheByUsername(String username) {
        appUserRepository.findByUsername(username).ifPresent(user -> evictBootstrapCache(user.getId()));
    }

    private Optional<ObjectNode> getCachedBootstrap(Long userId) {
        try {
            String payload = redisTemplate.opsForValue().get(cacheKey(userId));
            if (payload == null || payload.trim().isEmpty()) {
                return Optional.empty();
            }
            return Optional.of((ObjectNode) objectMapper.readTree(payload));
        } catch (Exception exception) {
            LOGGER.warn("Failed to read bootstrap cache: {}", exception.getMessage());
            return Optional.empty();
        }
    }

    private ObjectNode buildAndCacheBootstrap(SessionUser sessionUser) {
        ObjectNode root = platformTemplateService.copyTemplate();
        patchShell(root, sessionUser);
        patchOverview(root, sessionUser);
        patchOrders(root, sessionUser);
        patchTasks(root, sessionUser);
        patchMessages(root);
        cacheBootstrap(sessionUser.getUserId(), root);
        return root;
    }

    private void patchShell(ObjectNode root, SessionUser sessionUser) {
        ObjectNode userNode = (ObjectNode) root.with("shell").with("user");
        userNode.put("account", sessionUser.getUsername());
        userNode.put("displayName", sessionUser.getDisplayName());
    }

    private void patchOverview(ObjectNode root, SessionUser sessionUser) {
        ObjectNode overviewData = root.with("overviewData");
        ObjectNode userPanel = overviewData.with("userPanel");

        long submittedCount = workOrderRepository.countByCreatorUsername(sessionUser.getUsername());
        long pendingCount = taskRecordRepository.countByAssigneeAndStatusNot(sessionUser.getDisplayName(), "已完成");

        userPanel.put("account", sessionUser.getUsername());
        userPanel.put("displayName", sessionUser.getDisplayName());
        userPanel.put("todoSubmitted", submittedCount);
        userPanel.put("todoPending", pendingCount);

        List<NotificationMessage> latestMessages = notificationMessageRepository.findTop3ByOrderBySentAtDesc();
        if (!latestMessages.isEmpty()) {
            ArrayNode arrayNode = objectMapper.createArrayNode();
            latestMessages.forEach(message -> {
                ObjectNode node = objectMapper.createObjectNode();
                node.put("title", message.getTitle());
                node.put("time", message.getSentAt().format(DateFormats.SECOND_PRECISION));
                arrayNode.add(node);
            });
            overviewData.set("latestMessages", arrayNode);
        }
    }

    private void patchOrders(ObjectNode root, SessionUser sessionUser) {
        ObjectNode page = (ObjectNode) root.with("pages").get("/personSetting/orderManage");
        List<WorkOrder> orders = workOrderRepository.findTop10ByCreatorUsernameOrderByCreatedAtDesc(sessionUser.getUsername());
        ArrayNode rows = objectMapper.createArrayNode();
        orders.forEach(order -> {
            ObjectNode node = objectMapper.createObjectNode();
            node.put("id", order.getOrderNo());
            node.put("title", order.getTitle());
            node.put("type", order.getType());
            node.put("creator", order.getCreatorDisplayName());
            node.put("progress", order.getProgress());
            node.put("status", order.getStatus());
            node.put("eta", order.getEstimatedAt());
            node.put("createdAt", order.getCreatedAt().format(DateFormats.MINUTE_PRECISION));
            rows.add(node);
        });
        page.set("rows", rows);

        ArrayNode tabs = objectMapper.createArrayNode();
        tabs.add("我提交的(" + workOrderRepository.countByCreatorUsername(sessionUser.getUsername()) + ")");
        tabs.add("我的待办");
        tabs.add("我的已办");
        tabs.add("所有工单(" + workOrderRepository.count() + ")");
        tabs.add("草稿箱(0)");
        page.set("tabs", tabs);
    }

    private void patchTasks(ObjectNode root, SessionUser sessionUser) {
        ObjectNode page = (ObjectNode) root.with("pages").get("/personSetting/taskManage");
        List<TaskRecord> tasks = taskRecordRepository.findTop10ByOrderByCreatedAtDesc();
        ArrayNode rows = objectMapper.createArrayNode();
        tasks.forEach(task -> {
            ObjectNode node = objectMapper.createObjectNode();
            node.put("id", task.getTaskNo());
            node.put("title", task.getTitle());
            node.put("source", task.getSource());
            node.put("ticket", task.getTicket());
            node.put("status", task.getStatus());
            node.put("assignee", task.getAssignee());
            node.put("priority", task.getPriority());
            node.put("creator", task.getCreator());
            node.put("createdAt", task.getCreatedAt().format(DateFormats.MINUTE_PRECISION));
            rows.add(node);
        });
        page.set("rows", rows);

        long todoCount = taskRecordRepository.countByAssigneeAndStatusNot(sessionUser.getDisplayName(), "已完成");
        long doneCount = taskRecordRepository.countByAssigneeAndStatus(sessionUser.getDisplayName(), "已完成");
        long createdCount = taskRecordRepository.countByCreator(sessionUser.getDisplayName());
        ArrayNode tabs = objectMapper.createArrayNode();
        tabs.add("我的待办(" + todoCount + ")");
        tabs.add("我的已办(" + doneCount + ")");
        tabs.add("我创建的(" + createdCount + ")");
        tabs.add("所有任务(" + taskRecordRepository.count() + ")");
        tabs.add("任务模板(0)");
        page.set("tabs", tabs);
    }

    private void patchMessages(ObjectNode root) {
        ObjectNode page = (ObjectNode) root.with("pages").get("/msgCenter/messageManage");
        List<NotificationMessage> messages = notificationMessageRepository.findTop10ByOrderBySentAtDesc();
        ArrayNode rows = objectMapper.createArrayNode();
        messages.forEach(message -> {
            ObjectNode node = objectMapper.createObjectNode();
            node.put("title", message.getTitle());
            node.put("time", message.getSentAt().format(DateFormats.SECOND_PRECISION));
            node.put("type", message.getMessageType());
            rows.add(node);
        });
        page.set("rows", rows);
    }

    private void cacheBootstrap(Long userId, ObjectNode root) {
        try {
            redisTemplate.opsForValue().set(
                cacheKey(userId),
                objectMapper.writeValueAsString(root),
                Duration.ofMinutes(appProperties.getCache().getBootstrapTtlMinutes())
            );
        } catch (Exception exception) {
            LOGGER.warn("Failed to cache bootstrap payload: {}", exception.getMessage());
        }
    }

    private String cacheKey(Long userId) {
        return BOOTSTRAP_CACHE_KEY_PREFIX + userId;
    }
}
