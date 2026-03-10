package com.opsany.replica.service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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
import com.opsany.replica.domain.WorkOrderCatalog;
import com.opsany.replica.domain.WorkOrderProcessDefinition;
import com.opsany.replica.dto.NavigationGroupDto;
import com.opsany.replica.dto.NavigationItemDto;
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

    private final PlatformShellTemplateService platformShellTemplateService;
    private final MenuPermissionService menuPermissionService;
    private final MessageSubscriptionService messageSubscriptionService;
    private final DutyScheduleService dutyScheduleService;
    private final WorkOrderCatalogService workOrderCatalogService;
    private final WorkOrderProcessService workOrderProcessService;
    private final PlatformNavigationService platformNavigationService;
    private final PlatformPageStateService platformPageStateService;
    private final WorkOrderRepository workOrderRepository;
    private final TaskRecordRepository taskRecordRepository;
    private final NotificationMessageRepository notificationMessageRepository;
    private final AppUserRepository appUserRepository;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final AppProperties appProperties;

    public ObjectNode getBootstrap(SessionUser sessionUser, String requestPath) {
        List<NavigationGroupDto> navigationGroups = platformNavigationService.getNavigationGroups(sessionUser.getUserId());
        PlatformContext context = resolvePlatformContext(requestPath, navigationGroups);
        return getCachedBootstrap(sessionUser.getUserId(), context.getPlatformKey())
            .orElseGet(() -> buildAndCacheBootstrap(sessionUser, context, navigationGroups));
    }

    public void evictBootstrapCache(Long userId) {
        if (!appProperties.getCache().isBootstrapEnabled()) {
            return;
        }
        try {
            Set<String> keys = redisTemplate.keys(BOOTSTRAP_CACHE_KEY_PREFIX + userId + ":*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
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

    public void evictAllBootstrapCaches() {
        if (!appProperties.getCache().isBootstrapEnabled()) {
            return;
        }
        try {
            Set<String> keys = redisTemplate.keys(BOOTSTRAP_CACHE_KEY_PREFIX + "*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (Exception exception) {
            LOGGER.warn("Failed to evict all bootstrap caches: {}", exception.getMessage());
        }
    }

    private Optional<ObjectNode> getCachedBootstrap(Long userId, String platformKey) {
        if (!appProperties.getCache().isBootstrapEnabled()) {
            return Optional.empty();
        }
        try {
            String payload = redisTemplate.opsForValue().get(cacheKey(userId, platformKey));
            if (payload == null || payload.trim().isEmpty()) {
                return Optional.empty();
            }
            return Optional.of((ObjectNode) objectMapper.readTree(payload));
        } catch (Exception exception) {
            LOGGER.warn("Failed to read bootstrap cache: {}", exception.getMessage());
            return Optional.empty();
        }
    }

    private ObjectNode buildAndCacheBootstrap(
        SessionUser sessionUser,
        PlatformContext context,
        List<NavigationGroupDto> navigationGroups
    ) {
        ObjectNode root = platformShellTemplateService.copyTemplate(
            context.getPlatformKey(),
            context.getPlatformName(),
            context.getPlatformDescription(),
            context.getBasePath()
        );
        patchShell(root, sessionUser, context);
        patchNavigation(root, navigationGroups);
        platformPageStateService.ensureTableKeys(root.with("pages"));

        if ("workbench".equals(context.getPlatformKey())) {
            List<MenuPermission> menus = menuPermissionService.findMenusByUserId(sessionUser.getUserId());
            patchMenus(root, menus);
            filterPages(root, menus);
            patchCatalogs(root);
            patchOverview(root, sessionUser);
            patchOrders(root, sessionUser);
            patchTasks(root, sessionUser);
            patchMessages(root);
            patchSubscriptions(root, sessionUser);
            patchDuty(root, sessionUser);
            patchProcesses(root);
        }

        platformPageStateService.applyState(root, context.getPlatformKey());
        cacheBootstrap(sessionUser.getUserId(), context.getPlatformKey(), root);
        return root;
    }

    private void patchShell(ObjectNode root, SessionUser sessionUser, PlatformContext context) {
        ObjectNode shellNode = root.with("shell");
        shellNode.put("productName", context.getPlatformName());
        shellNode.put("platformButton", "平台导航");
        shellNode.put("basePath", context.getBasePath());
        shellNode.put("platformKey", context.getPlatformKey());
        if (!shellNode.has("topNav") || !shellNode.get("topNav").isArray()) {
            ArrayNode topNav = shellNode.putArray("topNav");
            topNav.add("控制台");
            topNav.add("工作台");
            topNav.add("消息");
            topNav.add("支持");
        }

        ObjectNode userNode = (ObjectNode) shellNode.with("user");
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

    private void patchCatalogs(ObjectNode root) {
        List<WorkOrderCatalog> onlineCatalogs = workOrderCatalogService.listOnlineCatalogs();
        List<WorkOrderCatalog> allCatalogs = workOrderCatalogService.listAll();

        if (root.with("pages").has("/personSetting/serviceFolder")) {
            ObjectNode page = (ObjectNode) root.with("pages").get("/personSetting/serviceFolder");
            ArrayNode services = objectMapper.createArrayNode();
            Map<String, Integer> categoryCounter = new LinkedHashMap<String, Integer>();
            for (WorkOrderCatalog catalog : onlineCatalogs) {
                ObjectNode node = objectMapper.createObjectNode();
                node.put("title", catalog.getName());
                node.put("type", catalog.getType());
                node.put("description", defaultText(catalog.getDescription(), "--"));
                node.put("count", workOrderRepository.countByServiceName(catalog.getName()) + " 人已提单");
                services.add(node);
                String category = defaultText(catalog.getCategory(), "默认分组");
                categoryCounter.put(category, categoryCounter.containsKey(category) ? categoryCounter.get(category) + 1 : 1);
            }
            page.set("services", services);
            page.set("serviceCategories", buildCategoryArray(onlineCatalogs.size(), categoryCounter));

            ArrayNode tabs = objectMapper.createArrayNode();
            tabs.add("全部服务(" + onlineCatalogs.size() + ")");
            tabs.add("我的收藏(0)");
            for (Map.Entry<String, Integer> entry : categoryCounter.entrySet()) {
                tabs.add(entry.getKey() + "(" + entry.getValue() + ")");
            }
            page.set("tabs", tabs);
        }

        if (root.with("pages").has("/serveSetting/orderDirectory")) {
            ObjectNode page = (ObjectNode) root.with("pages").get("/serveSetting/orderDirectory");
            ArrayNode rows = objectMapper.createArrayNode();
            Map<String, String> processNames = new LinkedHashMap<String, String>();
            for (WorkOrderProcessDefinition definition : workOrderProcessService.listDefinitions()) {
                processNames.put(definition.getProcessCode(), definition.getName());
            }
            Map<String, Integer> categoryCounter = new LinkedHashMap<String, Integer>();
            for (WorkOrderCatalog catalog : allCatalogs) {
                String category = defaultText(catalog.getCategory(), "默认分组");
                categoryCounter.put(category, categoryCounter.containsKey(category) ? categoryCounter.get(category) + 1 : 1);

                ObjectNode node = objectMapper.createObjectNode();
                node.put("catalogCode", catalog.getCatalogCode());
                node.put("name", catalog.getName());
                node.put("type", catalog.getType());
                node.put("category", category);
                node.put("scope", catalog.getScope());
                node.put("online", Boolean.TRUE.equals(catalog.getOnline()) ? "已上线" : "未上线");
                node.put("version", defaultText(processNames.get(catalog.getProcessCode()), defaultText(catalog.getProcessCode(), "-")));
                node.put("processCode", catalog.getProcessCode());
                node.put("sla", defaultText(catalog.getSlaName(), "-"));
                node.put("owner", catalog.getOwnerDisplayName());
                node.put("ownerUsername", catalog.getOwnerUsername());
                node.put("createdAt", catalog.getCreatedAt().format(DateFormats.DAY_PRECISION));
                node.put("desc", defaultText(catalog.getDescription(), "-"));
                rows.add(node);
            }
            page.set("rows", rows);
            page.set("categories", buildCategoryArray(allCatalogs.size(), categoryCounter));
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
                node.put("description", defaultText(group.getDescription(), "-"));
                groups.add(node);
            }
            dutyManagePage.set("groups", groups);

            ArrayNode shifts = objectMapper.createArrayNode();
            for (DutyShift shift : dutyScheduleService.listAllShifts()) {
                ObjectNode node = objectMapper.createObjectNode();
                node.put("id", shift.getId());
                node.put("groupId", shift.getGroupId());
                node.put("group", shift.getGroupName());
                node.put("date", shift.getDutyDate().toString());
                node.put("label", shift.getDateLabel());
                node.put("time", shift.getShiftTime());
                node.put("owner", shift.getOwnerDisplayName());
                node.put("ownerUsername", shift.getOwnerUsername());
                node.put("status", shift.getStatus());
                shifts.add(node);
            }
            dutyManagePage.set("shifts", shifts);
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

    private void patchNavigation(ObjectNode root, List<NavigationGroupDto> groups) {
        ArrayNode groupArray = objectMapper.createArrayNode();
        ArrayNode favorites = objectMapper.createArrayNode();

        for (NavigationGroupDto group : groups) {
            ObjectNode groupNode = objectMapper.createObjectNode();
            groupNode.put("groupCode", group.getGroupCode());
            groupNode.put("title", group.getTitle());
            groupNode.put("sortNo", group.getSortNo() == null ? 0 : group.getSortNo());
            ArrayNode rows = objectMapper.createArrayNode();

            for (NavigationItemDto item : group.getRows()) {
                ObjectNode row = objectMapper.createObjectNode();
                row.put("id", item.getId() == null ? 0 : item.getId());
                row.put("itemCode", item.getItemCode());
                row.put("groupCode", item.getGroupCode());
                row.put("name", item.getName());
                row.put("icon", defaultText(item.getIcon(), firstCharacter(item.getName(), "导")));
                row.put("creator", defaultText(item.getCreatorDisplayName(), item.getCreatorUsername()));
                row.put("creatorUsername", defaultText(item.getCreatorUsername(), "system"));
                row.put("link", item.getLink());
                row.put("mobileVisible", Boolean.TRUE.equals(item.getMobileVisible()));
                row.put("mobile", Boolean.TRUE.equals(item.getMobileVisible()) ? "是" : "否");
                row.put("desc", defaultText(item.getDescription(), "平台导航入口"));
                row.put("sortNo", item.getSortNo() == null ? 0 : item.getSortNo());
                row.put("enabled", Boolean.TRUE.equals(item.getEnabled()));
                row.put("favorite", Boolean.TRUE.equals(item.getFavorite()));
                rows.add(row);

                if (Boolean.TRUE.equals(item.getFavorite())) {
                    favorites.add(row.deepCopy());
                }
            }

            groupNode.set("rows", rows);
            groupArray.add(groupNode);
        }

        root.set("navigationGroups", groupArray);
        root.set("favoriteNavigations", favorites);
    }

    private void cacheBootstrap(Long userId, String platformKey, ObjectNode root) {
        if (!appProperties.getCache().isBootstrapEnabled()) {
            return;
        }
        try {
            redisTemplate.opsForValue().set(
                cacheKey(userId, platformKey),
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

    private String cacheKey(Long userId, String platformKey) {
        return BOOTSTRAP_CACHE_KEY_PREFIX + userId + ":" + defaultText(platformKey, "workbench");
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }

    private String firstCharacter(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.substring(0, 1);
    }

    private ArrayNode buildCategoryArray(int total, Map<String, Integer> categoryCounter) {
        ArrayNode arrayNode = objectMapper.createArrayNode();
        arrayNode.add("所有服务(" + total + ")");
        for (Map.Entry<String, Integer> entry : categoryCounter.entrySet()) {
            arrayNode.add(entry.getKey() + "(" + entry.getValue() + ")");
        }
        return arrayNode;
    }

    private PlatformContext resolvePlatformContext(String requestPath, List<NavigationGroupDto> navigationGroups) {
        String basePath = resolveBasePath(requestPath);
        String platformKey = extractPlatformKey(basePath);
        NavigationItemDto currentItem = findNavigationItem(navigationGroups, basePath);
        String fallbackName = fallbackPlatformName(platformKey);
        String platformName = currentItem == null ? fallbackName : defaultText(currentItem.getName(), fallbackName);
        String platformDescription = currentItem == null
            ? platformName + " 平台入口"
            : defaultText(currentItem.getDescription(), platformName + " 平台入口");
        return new PlatformContext(platformKey, basePath, platformName, platformDescription);
    }

    private NavigationItemDto findNavigationItem(List<NavigationGroupDto> navigationGroups, String basePath) {
        String normalizedBasePath = resolveBasePath(basePath);
        for (NavigationGroupDto group : navigationGroups) {
            for (NavigationItemDto item : group.getRows()) {
                if (normalizedBasePath.equals(resolveBasePath(item.getLink()))) {
                    return item;
                }
            }
        }
        return null;
    }

    private String resolveBasePath(String requestPath) {
        String normalizedPath = normalizePath(requestPath);
        if (!normalizedPath.startsWith("/o/")) {
            return "/o/workbench";
        }

        String[] segments = normalizedPath.split("/");
        if (segments.length < 3 || segments[2] == null || segments[2].trim().isEmpty()) {
            return "/o/workbench";
        }
        return "/o/" + segments[2];
    }

    private String extractPlatformKey(String basePath) {
        String[] segments = resolveBasePath(basePath).split("/");
        return segments.length >= 3 ? defaultText(segments[2], "workbench") : "workbench";
    }

    private String normalizePath(String value) {
        if (value == null || value.trim().isEmpty() || "/".equals(value.trim())) {
            return "/o/workbench";
        }
        String normalized = value.trim().replace('#', '/');
        while (normalized.contains("//")) {
            normalized = normalized.replace("//", "/");
        }
        if (normalized.endsWith("/") && normalized.length() > 1) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private String fallbackPlatformName(String platformKey) {
        if ("cmdb".equals(platformKey)) {
            return "资源平台";
        }
        if ("job".equals(platformKey)) {
            return "作业平台";
        }
        if ("control".equals(platformKey)) {
            return "管控平台";
        }
        if ("bastion".equals(platformKey)) {
            return "堡垒机";
        }
        if ("workbench".equals(platformKey)) {
            return "工作台";
        }
        return platformKey == null || platformKey.trim().isEmpty() ? "平台" : platformKey.toUpperCase();
    }

    private static final class PlatformContext {

        private final String platformKey;
        private final String basePath;
        private final String platformName;
        private final String platformDescription;

        private PlatformContext(String platformKey, String basePath, String platformName, String platformDescription) {
            this.platformKey = platformKey;
            this.basePath = basePath;
            this.platformName = platformName;
            this.platformDescription = platformDescription;
        }

        private String getPlatformKey() {
            return platformKey;
        }

        private String getBasePath() {
            return basePath;
        }

        private String getPlatformName() {
            return platformName;
        }

        private String getPlatformDescription() {
            return platformDescription;
        }
    }
}
