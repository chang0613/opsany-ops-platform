package com.opsany.replica.service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.opsany.replica.config.AppProperties;
import com.opsany.replica.domain.AppUser;
import com.opsany.replica.domain.DutyGroup;
import com.opsany.replica.domain.DutyShift;
import com.opsany.replica.domain.MenuPermission;
import com.opsany.replica.domain.MessageSubscription;
import com.opsany.replica.domain.NotificationMessage;
import com.opsany.replica.domain.TaskRecord;
import com.opsany.replica.domain.WorkOrder;
import com.opsany.replica.domain.WorkOrderProcessDefinition;
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
    private final MenuPermissionService menuPermissionService;
    private final MessageSubscriptionService messageSubscriptionService;
    private final DutyScheduleService dutyScheduleService;
    private final WorkOrderProcessService workOrderProcessService;
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
        if (!appProperties.getCache().isBootstrapEnabled()) {
            return;
        }
        try {
            redisTemplate.delete(cacheKey(userId));
        } catch (Exception exception) {
            LOGGER.warn("Failed to evict bootstrap cache: {}", exception.getMessage());
        }
    }

    public void evictBootstrapCacheByUsername(String username) {
        AppUser user = appUserRepository.findByUsername(username);
        if (user != null) {
            evictBootstrapCache(user.getId());
        }
    }

    private Optional<ObjectNode> getCachedBootstrap(Long userId) {
        if (!appProperties.getCache().isBootstrapEnabled()) {
            return Optional.empty();
        }
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
        List<MenuPermission> menus = menuPermissionService.findMenusByUserId(sessionUser.getUserId());
        patchShell(root, sessionUser);
        patchMenus(root, menus);
        filterPages(root, menus);
        patchOverview(root, sessionUser);
        patchOrders(root, sessionUser);
        patchTasks(root, sessionUser);
        patchMessages(root);
        patchSubscriptions(root, sessionUser);
        patchDuty(root, sessionUser);
        patchProcesses(root);
        cacheBootstrap(sessionUser.getUserId(), root);
        return root;
    }

    private void patchShell(ObjectNode root, SessionUser sessionUser) {
        ObjectNode userNode = (ObjectNode) root.with("shell").with("user");
        userNode.put("account", sessionUser.getUsername());
        userNode.put("displayName", sessionUser.getDisplayName());
    }

    private void patchMenus(ObjectNode root, List<MenuPermission> menus) {
        Map<String, ArrayNode> grouped = new LinkedHashMap<String, ArrayNode>();
        for (MenuPermission menu : menus) {
            ArrayNode items = grouped.computeIfAbsent(menu.getGroupName(), ignored -> objectMapper.createArrayNode());
            ObjectNode node = objectMapper.createObjectNode();
            node.put("label", menu.getLabel());
            node.put("route", menu.getRoute());
            items.add(node);
        }

        ArrayNode groups = objectMapper.createArrayNode();
        for (Map.Entry<String, ArrayNode> entry : grouped.entrySet()) {
            ObjectNode group = objectMapper.createObjectNode();
            group.put("group", entry.getKey());
            group.set("items", entry.getValue());
            groups.add(group);
        }
        root.set("menu", groups);
    }

    private void filterPages(ObjectNode root, List<MenuPermission> menus) {
        ObjectNode pages = root.with("pages");
        Map<String, String> allowedRoutes = new LinkedHashMap<String, String>();
        allowedRoutes.put("/", "/");
        for (MenuPermission menu : menus) {
            allowedRoutes.put(menu.getRoute(), menu.getRoute());
        }

        List<String> routes = new ArrayList<String>();
        java.util.Iterator<String> iterator = pages.fieldNames();
        while (iterator.hasNext()) {
            routes.add(iterator.next());
        }
        for (String route : routes) {
            if (!allowedRoutes.containsKey(route)) {
                pages.remove(route);
            }
        }
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
        if (!root.with("pages").has("/personSetting/orderManage")) {
            return;
        }

        ObjectNode page = (ObjectNode) root.with("pages").get("/personSetting/orderManage");
        List<WorkOrder> orders = isAdmin(sessionUser)
            ? workOrderRepository.findTop20OrderByCreatedAtDesc()
            : workOrderRepository.findTop10ByCreatorUsernameOrderByCreatedAtDesc(sessionUser.getUsername());
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
            node.put("handler", defaultText(order.getCurrentHandler(), "--"));
            rows.add(node);
        });
        page.set("rows", rows);

        ArrayNode tabs = objectMapper.createArrayNode();
        tabs.add("我提交的(" + workOrderRepository.countByCreatorUsername(sessionUser.getUsername()) + ")");
        tabs.add("我的待办(" + taskRecordRepository.countByAssigneeAndStatusNot(sessionUser.getDisplayName(), "已完成") + ")");
        tabs.add("我的已办(" + taskRecordRepository.countByAssigneeAndStatus(sessionUser.getDisplayName(), "已完成") + ")");
        tabs.add("所有工单(" + workOrderRepository.count() + ")");
        tabs.add("草稿箱(0)");
        page.set("tabs", tabs);
    }

    private void patchTasks(ObjectNode root, SessionUser sessionUser) {
        if (!root.with("pages").has("/personSetting/taskManage")) {
            return;
        }

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
        if (!root.with("pages").has("/msgCenter/messageManage")) {
            return;
        }
        ObjectNode page = (ObjectNode) root.with("pages").get("/msgCenter/messageManage");
        List<NotificationMessage> messages = notificationMessageRepository.findTop10ByOrderBySentAtDesc();
        ArrayNode rows = objectMapper.createArrayNode();
        messages.forEach(message -> {
            ObjectNode node = objectMapper.createObjectNode();
            node.put("title", message.getTitle());
            node.put("time", message.getSentAt().format(DateFormats.SECOND_PRECISION));
            node.put("type", message.getMessageType());
            node.put("read", message.isRead());
            rows.add(node);
        });
        page.set("rows", rows);
    }

    private void patchSubscriptions(ObjectNode root, SessionUser sessionUser) {
        if (!root.with("pages").has("/msgCenter/subscriptionSetting")) {
            return;
        }

        List<MessageSubscription> subscriptions = messageSubscriptionService.listByUsername(sessionUser.getUsername());
        ObjectNode page = (ObjectNode) root.with("pages").get("/msgCenter/subscriptionSetting");
        ArrayNode rows = objectMapper.createArrayNode();
        subscriptions.forEach(subscription -> {
            ObjectNode node = objectMapper.createObjectNode();
            node.put("type", subscription.getMessageType());
            node.put("source", subscription.getSource());
            node.put("siteEnabled", Boolean.TRUE.equals(subscription.getSiteEnabled()));
            node.put("smsEnabled", Boolean.TRUE.equals(subscription.getSmsEnabled()));
            node.put("mailEnabled", Boolean.TRUE.equals(subscription.getMailEnabled()));
            node.put("wxEnabled", Boolean.TRUE.equals(subscription.getWxEnabled()));
            node.put("dingEnabled", Boolean.TRUE.equals(subscription.getDingEnabled()));
            rows.add(node);
        });
        page.set("rows", rows);
    }

    private void patchDuty(ObjectNode root, SessionUser sessionUser) {
        if (root.with("pages").has("/personSetting/myDuty")) {
            ObjectNode myDutyPage = (ObjectNode) root.with("pages").get("/personSetting/myDuty");
            ArrayNode shifts = objectMapper.createArrayNode();
            for (DutyShift shift : dutyScheduleService.listByOwnerUsername(sessionUser.getUsername())) {
                ObjectNode node = objectMapper.createObjectNode();
                node.put("date", shift.getDutyDate().toString());
                node.put("label", shift.getDateLabel());
                node.put("group", shift.getGroupName());
                node.put("owner", shift.getOwnerDisplayName());
                node.put("time", shift.getShiftTime());
                node.put("status", shift.getStatus());
                shifts.add(node);
            }
            myDutyPage.set("shifts", shifts);
        }

        if (root.with("pages").has("/serveSetting/dutyManage")) {
            ObjectNode dutyManagePage = (ObjectNode) root.with("pages").get("/serveSetting/dutyManage");
            ArrayNode groups = objectMapper.createArrayNode();
            for (DutyGroup group : dutyScheduleService.listGroups()) {
                ObjectNode node = objectMapper.createObjectNode();
                node.put("name", group.getName());
                node.put("owner", group.getOwnerDisplayName());
                node.put("members", group.getMembers());
                node.put("coverage", group.getCoverage());
                groups.add(node);
            }
            dutyManagePage.set("groups", groups);
        }
    }

    private void patchProcesses(ObjectNode root) {
        if (!root.with("pages").has("/serveSetting/orderProcess")) {
            return;
        }

        ObjectNode page = (ObjectNode) root.with("pages").get("/serveSetting/orderProcess");
        ArrayNode rows = objectMapper.createArrayNode();
        for (WorkOrderProcessDefinition process : workOrderProcessService.listDefinitions()) {
            ObjectNode node = objectMapper.createObjectNode();
            node.put("name", process.getName());
            node.put("owner", process.getOwner());
            node.put("creator", process.getCreator());
            node.put("updater", process.getUpdater());
            node.put("status", process.getStatus());
            node.put("updatedAt", process.getUpdatedAt().format(DateFormats.MINUTE_PRECISION));
            node.put("desc", defaultText(process.getDescription(), "-"));
            rows.add(node);
        }
        page.set("rows", rows);
    }

    private void cacheBootstrap(Long userId, ObjectNode root) {
        if (!appProperties.getCache().isBootstrapEnabled()) {
            return;
        }
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

    private boolean isAdmin(SessionUser sessionUser) {
        return sessionUser.getRoleCodes() != null && sessionUser.getRoleCodes().contains("PLATFORM_ADMIN");
    }

    private String cacheKey(Long userId) {
        return BOOTSTRAP_CACHE_KEY_PREFIX + userId;
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }
}
