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
import com.opsany.replica.domain.AlertEvent;
import com.opsany.replica.domain.AlertRule;
import com.opsany.replica.domain.AppUser;
import com.opsany.replica.domain.ArchiveHistory;
import com.opsany.replica.domain.ArchivePolicy;
import com.opsany.replica.domain.BigScreen;
import com.opsany.replica.domain.ChartTemplate;
import com.opsany.replica.domain.CollectionTemplate;
import com.opsany.replica.domain.Dashboard;
import com.opsany.replica.domain.DutyGroup;
import com.opsany.replica.domain.DutyShift;
import com.opsany.replica.domain.InfraServer;
import com.opsany.replica.domain.K8sCluster;
import com.opsany.replica.domain.K8sPod;
import com.opsany.replica.domain.LogCollector;
import com.opsany.replica.domain.MenuPermission;
import com.opsany.replica.domain.MessageSubscription;
import com.opsany.replica.domain.MetricSnapshot;
import com.opsany.replica.domain.MiddlewareInstance;
import com.opsany.replica.domain.NetworkDevice;
import com.opsany.replica.domain.NotificationMessage;
import com.opsany.replica.domain.NotifyChannel;
import com.opsany.replica.domain.OnCallSchedule;
import com.opsany.replica.domain.ResponseRecord;
import com.opsany.replica.domain.ServiceProbe;
import com.opsany.replica.domain.StorageNode;
import com.opsany.replica.domain.TaskRecord;
import com.opsany.replica.domain.TopologyItem;
import com.opsany.replica.domain.TopologyLink;
import com.opsany.replica.domain.WorkOrder;
import com.opsany.replica.domain.WorkOrderCatalog;
import com.opsany.replica.domain.WorkOrderProcessDefinition;
import com.opsany.replica.dto.NavigationGroupDto;
import com.opsany.replica.dto.NavigationItemDto;
import com.opsany.replica.repository.AlertEventRepository;
import com.opsany.replica.repository.AlertRuleRepository;
import com.opsany.replica.repository.AppUserRepository;
import com.opsany.replica.repository.ArchiveHistoryRepository;
import com.opsany.replica.repository.ArchivePolicyRepository;
import com.opsany.replica.repository.BigScreenRepository;
import com.opsany.replica.repository.ChartTemplateRepository;
import com.opsany.replica.repository.DashboardRepository;
import com.opsany.replica.repository.InfraServerRepository;
import com.opsany.replica.repository.K8sClusterRepository;
import com.opsany.replica.repository.K8sPodRepository;
import com.opsany.replica.repository.LogCollectorRepository;
import com.opsany.replica.repository.MiddlewareInstanceRepository;
import com.opsany.replica.repository.NotifyChannelRepository;
import com.opsany.replica.repository.OnCallScheduleRepository;
import com.opsany.replica.repository.NotificationMessageRepository;
import com.opsany.replica.repository.ResponseRecordRepository;
import com.opsany.replica.repository.ServiceProbeRepository;
import com.opsany.replica.repository.StorageNodeRepository;
import com.opsany.replica.repository.TaskRecordRepository;
import com.opsany.replica.repository.TopologyItemRepository;
import com.opsany.replica.repository.TopologyLinkRepository;
import com.opsany.replica.repository.WorkOrderRepository;
import com.opsany.replica.security.SessionUser;
import com.opsany.replica.service.NetworkCollectionService;

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
    private final NetworkCollectionService networkCollectionService;
    private final AlertRuleRepository alertRuleRepository;
    private final ServiceProbeRepository serviceProbeRepository;
    private final LogCollectorRepository logCollectorRepository;
    private final WorkOrderRepository workOrderRepository;
    private final TaskRecordRepository taskRecordRepository;
    private final NotificationMessageRepository notificationMessageRepository;
    private final AppUserRepository appUserRepository;
    private final InfraServerRepository infraServerRepository;
    private final K8sClusterRepository k8sClusterRepository;
    private final K8sPodRepository k8sPodRepository;
    private final MiddlewareInstanceRepository middlewareInstanceRepository;
    private final StorageNodeRepository storageNodeRepository;
    private final ArchivePolicyRepository archivePolicyRepository;
    private final ArchiveHistoryRepository archiveHistoryRepository;
    private final NotifyChannelRepository notifyChannelRepository;
    private final AlertEventRepository alertEventRepository;
    private final OnCallScheduleRepository onCallScheduleRepository;
    private final ResponseRecordRepository responseRecordRepository;
    private final DashboardRepository dashboardRepository;
    private final TopologyItemRepository topologyItemRepository;
    private final TopologyLinkRepository topologyLinkRepository;
    private final BigScreenRepository bigScreenRepository;
    private final ChartTemplateRepository chartTemplateRepository;
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
            patchNetworkDevicesPage(root);
            patchServiceProbesPage(root);
            patchLogCollectorsPage(root);
            patchAlertRulesPage(root);
            patchInfraMetricsPage(root);
            patchContainerMetricsPage(root);
            patchMiddlewareMetricsPage(root);
            patchDataStoragePage(root);
            patchDataGovernancePage(root);
            patchNotifyChannelsPage(root);
            patchAlertHistoryPage(root);
            patchOnCallMgmtPage(root);
            patchDashboardsPage(root);
            patchTopologyPage(root);
            patchChartsPage(root);
        }

        if ("monitor".equals(context.getPlatformKey())) {
            patchMonitorOverview(root);
            patchNetworkDevices(root);
            patchCollectionTemplates(root);
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

    private void patchNetworkDevicesPage(ObjectNode root) {
        if (!root.with("pages").has("/monitor/networkDevices")) {
            return;
        }
        ObjectNode page = (ObjectNode) root.with("pages").get("/monitor/networkDevices");

        long totalDevices = networkCollectionService.countDevices();
        long normalDevices = networkCollectionService.countDevicesByStatus("采集正常");
        long totalTemplates = networkCollectionService.countTemplates();
        long discoveredDevices = networkCollectionService.countDevicesByStatus("已发现");

        ArrayNode stats = objectMapper.createArrayNode();
        ObjectNode s1 = objectMapper.createObjectNode();
        s1.put("label", "网络设备总数"); s1.put("value", String.valueOf(totalDevices)); s1.put("hint", "路由器 / 交换机 / 防火墙");
        stats.add(s1);
        ObjectNode s2 = objectMapper.createObjectNode();
        s2.put("label", "采集正常"); s2.put("value", String.valueOf(normalDevices)); s2.put("hint", "SNMP/SSH 采集成功");
        stats.add(s2);
        ObjectNode s3 = objectMapper.createObjectNode();
        s3.put("label", "采集模板"); s3.put("value", String.valueOf(totalTemplates)); s3.put("hint", "自定义采集指标集");
        stats.add(s3);
        ObjectNode s4 = objectMapper.createObjectNode();
        s4.put("label", "待纳管设备"); s4.put("value", String.valueOf(discoveredDevices)); s4.put("hint", "已发现未录入");
        stats.add(s4);
        page.set("stats", stats);

        List<NetworkDevice> devices = networkCollectionService.listDevices();
        ArrayNode deviceRows = objectMapper.createArrayNode();
        for (NetworkDevice device : devices) {
            ObjectNode node = objectMapper.createObjectNode();
            node.put("name", device.getName());
            node.put("ip", device.getIp());
            node.put("deviceType", device.getDeviceType());
            node.put("vendor", device.getVendor());
            node.put("protocol", device.getProtocol());
            node.put("port", device.getPort());
            node.put("status", device.getStatus());
            node.put("lastCollectedAt", device.getLastCollectedAt() != null
                ? device.getLastCollectedAt().format(DateFormats.MINUTE_PRECISION) : "-");
            node.put("description", defaultText(device.getDescription(), "-"));
            deviceRows.add(node);
        }

        List<MetricSnapshot> snapshots = networkCollectionService.getRecentMetrics();
        ArrayNode metricRows = objectMapper.createArrayNode();
        for (MetricSnapshot snapshot : snapshots) {
            ObjectNode node = objectMapper.createObjectNode();
            node.put("device", snapshot.getDeviceCode());
            node.put("metric", snapshot.getMetricName());
            node.put("value", snapshot.getMetricValue());
            node.put("time", snapshot.getCollectedAt() != null
                ? snapshot.getCollectedAt().format(DateFormats.SECOND_PRECISION) : "-");
            metricRows.add(node);
        }

        com.fasterxml.jackson.databind.JsonNode tables = page.get("tables");
        if (tables != null && tables.isArray()) {
            if (tables.size() > 0) ((ObjectNode) tables.get(0)).set("rows", deviceRows);
            if (tables.size() > 1) ((ObjectNode) tables.get(1)).set("rows", metricRows);
        }
    }

    private void patchServiceProbesPage(ObjectNode root) {
        if (!root.with("pages").has("/monitor/serviceProbe")) {
            return;
        }
        ObjectNode page = (ObjectNode) root.with("pages").get("/monitor/serviceProbe");

        long total = serviceProbeRepository.count();
        long running = serviceProbeRepository.countByStatus("运行中");
        long abnormal = serviceProbeRepository.countByStatus("异常");

        ArrayNode stats = objectMapper.createArrayNode();
        ObjectNode s1 = objectMapper.createObjectNode();
        s1.put("label", "探测任务总数"); s1.put("value", String.valueOf(total)); s1.put("hint", "HTTP / TCP / ICMP");
        stats.add(s1);
        ObjectNode s2 = objectMapper.createObjectNode();
        s2.put("label", "运行中"); s2.put("value", String.valueOf(running)); s2.put("hint", "正常采集");
        stats.add(s2);
        ObjectNode s3 = objectMapper.createObjectNode();
        s3.put("label", "探测异常"); s3.put("value", String.valueOf(abnormal)); s3.put("hint", "需关注");
        stats.add(s3);
        ObjectNode s4 = objectMapper.createObjectNode();
        s4.put("label", "平均响应时间"); s4.put("value", "128ms"); s4.put("hint", "最近1小时均值");
        stats.add(s4);
        page.set("stats", stats);

        List<ServiceProbe> probes = serviceProbeRepository.findAll();
        ArrayNode rows = objectMapper.createArrayNode();
        for (ServiceProbe probe : probes) {
            ObjectNode node = objectMapper.createObjectNode();
            node.put("name", probe.getName());
            node.put("probeType", probe.getProbeType());
            node.put("targetUrl", defaultText(probe.getTargetUrl(), "-"));
            node.put("protocol", defaultText(probe.getProtocol(), "-"));
            node.put("interval", probe.getIntervalSeconds() + "s");
            node.put("timeout", probe.getTimeoutMs() + "ms");
            node.put("status", defaultText(probe.getStatus(), "-"));
            node.put("lastResult", defaultText(probe.getLastResult(), "-"));
            node.put("lastCheckedAt", probe.getLastCheckedAt() != null
                ? probe.getLastCheckedAt().format(DateFormats.MINUTE_PRECISION) : "-");
            node.put("description", defaultText(probe.getDescription(), "-"));
            rows.add(node);
        }

        com.fasterxml.jackson.databind.JsonNode tables = page.get("tables");
        if (tables != null && tables.isArray() && tables.size() > 0) {
            ((ObjectNode) tables.get(0)).set("rows", rows);
        }
    }

    private void patchLogCollectorsPage(ObjectNode root) {
        if (!root.with("pages").has("/monitor/logCollection")) {
            return;
        }
        ObjectNode page = (ObjectNode) root.with("pages").get("/monitor/logCollection");

        long total = logCollectorRepository.count();
        long running = logCollectorRepository.countByStatus("运行中");
        long abnormal = logCollectorRepository.countByStatus("异常");

        ArrayNode stats = objectMapper.createArrayNode();
        ObjectNode s1 = objectMapper.createObjectNode();
        s1.put("label", "采集任务总数"); s1.put("value", String.valueOf(total)); s1.put("hint", "文件 / Syslog / API");
        stats.add(s1);
        ObjectNode s2 = objectMapper.createObjectNode();
        s2.put("label", "运行中"); s2.put("value", String.valueOf(running)); s2.put("hint", "正常采集");
        stats.add(s2);
        ObjectNode s3 = objectMapper.createObjectNode();
        s3.put("label", "采集异常"); s3.put("value", String.valueOf(abnormal)); s3.put("hint", "需关注");
        stats.add(s3);
        ObjectNode s4 = objectMapper.createObjectNode();
        s4.put("label", "今日采集行数"); s4.put("value", "1,284,320"); s4.put("hint", "全部采集任务累计");
        stats.add(s4);
        page.set("stats", stats);

        List<LogCollector> collectors = logCollectorRepository.findAll();
        ArrayNode rows = objectMapper.createArrayNode();
        for (LogCollector collector : collectors) {
            ObjectNode node = objectMapper.createObjectNode();
            node.put("name", collector.getName());
            node.put("sourceType", defaultText(collector.getSourceType(), "-"));
            node.put("sourcePath", defaultText(collector.getSourcePath(), "-"));
            node.put("targetHost", defaultText(collector.getTargetHost(), "-"));
            node.put("encoding", defaultText(collector.getEncoding(), "UTF-8"));
            node.put("status", defaultText(collector.getStatus(), "-"));
            node.put("linesCollected", collector.getLinesCollected());
            node.put("lastCollectedAt", collector.getLastCollectedAt() != null
                ? collector.getLastCollectedAt().format(DateFormats.MINUTE_PRECISION) : "-");
            node.put("description", defaultText(collector.getDescription(), "-"));
            rows.add(node);
        }

        com.fasterxml.jackson.databind.JsonNode tables = page.get("tables");
        if (tables != null && tables.isArray() && tables.size() > 0) {
            ((ObjectNode) tables.get(0)).set("rows", rows);
        }
    }

    private void patchAlertRulesPage(ObjectNode root) {
        if (!root.with("pages").has("/monitor/alertRules")) {
            return;
        }
        ObjectNode page = (ObjectNode) root.with("pages").get("/monitor/alertRules");

        long total = alertRuleRepository.count();
        long enabled = alertRuleRepository.countEnabled();

        ArrayNode stats = objectMapper.createArrayNode();
        ObjectNode s1 = objectMapper.createObjectNode();
        s1.put("label", "规则总数"); s1.put("value", String.valueOf(total)); s1.put("hint", "全部告警规则");
        stats.add(s1);
        ObjectNode s2 = objectMapper.createObjectNode();
        s2.put("label", "已启用"); s2.put("value", String.valueOf(enabled)); s2.put("hint", "生效中");
        stats.add(s2);
        ObjectNode s3 = objectMapper.createObjectNode();
        s3.put("label", "已停用"); s3.put("value", String.valueOf(total - enabled)); s3.put("hint", "暂未生效");
        stats.add(s3);
        ObjectNode s4 = objectMapper.createObjectNode();
        s4.put("label", "今日触发次数"); s4.put("value", "47"); s4.put("hint", "规则命中统计");
        stats.add(s4);
        page.set("stats", stats);

        List<AlertRule> rules = alertRuleRepository.findAll();
        ArrayNode rows = objectMapper.createArrayNode();
        for (AlertRule rule : rules) {
            ObjectNode node = objectMapper.createObjectNode();
            node.put("name", rule.getName());
            node.put("metricType", defaultText(rule.getMetricType(), "-"));
            node.put("conditionExpr", defaultText(rule.getConditionExpr(), "-"));
            node.put("threshold", defaultText(rule.getThreshold(), "-"));
            node.put("severity", defaultText(rule.getSeverity(), "-"));
            node.put("enabled", Boolean.TRUE.equals(rule.getEnabled()) ? "已启用" : "已停用");
            node.put("notifyGroup", defaultText(rule.getNotifyGroup(), "-"));
            node.put("description", defaultText(rule.getDescription(), "-"));
            rows.add(node);
        }

        com.fasterxml.jackson.databind.JsonNode tables = page.get("tables");
        if (tables != null && tables.isArray() && tables.size() > 0) {
            ((ObjectNode) tables.get(0)).set("rows", rows);
        }
    }

    private void patchInfraMetricsPage(ObjectNode root) {
        if (!root.with("pages").has("/monitor/infraMetrics")) {
            return;
        }
        ObjectNode page = (ObjectNode) root.with("pages").get("/monitor/infraMetrics");

        long total = infraServerRepository.count();
        long normal = infraServerRepository.countByStatus("正常");
        long alert = infraServerRepository.countByStatus("告警");

        ArrayNode stats = objectMapper.createArrayNode();
        ObjectNode s1 = objectMapper.createObjectNode();
        s1.put("label", "服务器总数"); s1.put("value", String.valueOf(total)); s1.put("hint", "物理机 / 虚拟机");
        stats.add(s1);
        ObjectNode s2 = objectMapper.createObjectNode();
        s2.put("label", "状态正常"); s2.put("value", String.valueOf(normal)); s2.put("hint", "指标正常");
        stats.add(s2);
        ObjectNode s3 = objectMapper.createObjectNode();
        s3.put("label", "告警中"); s3.put("value", String.valueOf(alert)); s3.put("hint", "需关注");
        stats.add(s3);
        ObjectNode s4 = objectMapper.createObjectNode();
        s4.put("label", "今日采集次数"); s4.put("value", String.valueOf(total * 288)); s4.put("hint", "5分钟一次");
        stats.add(s4);
        page.set("stats", stats);

        List<InfraServer> servers = infraServerRepository.findAll();
        ArrayNode rows = objectMapper.createArrayNode();
        for (InfraServer server : servers) {
            ObjectNode node = objectMapper.createObjectNode();
            node.put("hostname", server.getHostname());
            node.put("ip", server.getIp());
            node.put("model", defaultText(server.getModel(), "-"));
            node.put("cpuUsage", defaultText(server.getCpuUsage(), "-"));
            node.put("memUsage", defaultText(server.getMemUsage(), "-"));
            node.put("diskUsage", defaultText(server.getDiskUsage(), "-"));
            node.put("cpuTemp", defaultText(server.getCpuTemp(), "-"));
            node.put("collectMethod", defaultText(server.getCollectMethod(), "-"));
            node.put("status", server.getStatus());
            rows.add(node);
        }

        com.fasterxml.jackson.databind.JsonNode tables = page.get("tables");
        if (tables != null && tables.isArray() && tables.size() > 0) {
            ((ObjectNode) tables.get(0)).set("rows", rows);
        }
    }

    private void patchContainerMetricsPage(ObjectNode root) {
        if (!root.with("pages").has("/monitor/containerMetrics")) {
            return;
        }
        ObjectNode page = (ObjectNode) root.with("pages").get("/monitor/containerMetrics");

        long totalClusters = k8sClusterRepository.count();
        long normalClusters = k8sClusterRepository.countByStatus("正常");
        long totalPods = k8sPodRepository.count();
        long abnormalPods = k8sPodRepository.countByStatus("重启中") + k8sPodRepository.countByStatus("告警");

        ArrayNode stats = objectMapper.createArrayNode();
        ObjectNode s1 = objectMapper.createObjectNode();
        s1.put("label", "集群总数"); s1.put("value", String.valueOf(totalClusters)); s1.put("hint", "K8s集群");
        stats.add(s1);
        ObjectNode s2 = objectMapper.createObjectNode();
        s2.put("label", "正常集群"); s2.put("value", String.valueOf(normalClusters)); s2.put("hint", "运行正常");
        stats.add(s2);
        ObjectNode s3 = objectMapper.createObjectNode();
        s3.put("label", "Pod总数"); s3.put("value", String.valueOf(totalPods)); s3.put("hint", "所有命名空间");
        stats.add(s3);
        ObjectNode s4 = objectMapper.createObjectNode();
        s4.put("label", "异常Pod"); s4.put("value", String.valueOf(abnormalPods)); s4.put("hint", "需关注");
        stats.add(s4);
        page.set("stats", stats);

        List<K8sCluster> clusters = k8sClusterRepository.findAll();
        ArrayNode clusterRows = objectMapper.createArrayNode();
        for (K8sCluster cluster : clusters) {
            ObjectNode node = objectMapper.createObjectNode();
            node.put("clusterName", cluster.getClusterName());
            node.put("version", defaultText(cluster.getVersion(), "-"));
            node.put("nodeCount", cluster.getNodeCount());
            node.put("podCount", cluster.getPodCount());
            node.put("cpuUsage", defaultText(cluster.getCpuUsage(), "-"));
            node.put("memUsage", defaultText(cluster.getMemUsage(), "-"));
            node.put("status", cluster.getStatus());
            clusterRows.add(node);
        }

        List<K8sPod> pods = k8sPodRepository.findAll();
        ArrayNode podRows = objectMapper.createArrayNode();
        for (K8sPod pod : pods) {
            if ("重启中".equals(pod.getStatus()) || "告警".equals(pod.getStatus())) {
                ObjectNode node = objectMapper.createObjectNode();
                node.put("namespace", pod.getNamespace());
                node.put("podName", pod.getPodName());
                node.put("nodeName", defaultText(pod.getNodeName(), "-"));
                node.put("cpuUsage", defaultText(pod.getCpuUsage(), "-"));
                node.put("memUsage", defaultText(pod.getMemUsage(), "-"));
                node.put("restartCount", pod.getRestartCount());
                node.put("status", pod.getStatus());
                podRows.add(node);
            }
        }

        com.fasterxml.jackson.databind.JsonNode tables = page.get("tables");
        if (tables != null && tables.isArray()) {
            if (tables.size() > 0) ((ObjectNode) tables.get(0)).set("rows", clusterRows);
            if (tables.size() > 1) ((ObjectNode) tables.get(1)).set("rows", podRows);
        }
    }

    private void patchMiddlewareMetricsPage(ObjectNode root) {
        if (!root.with("pages").has("/monitor/middlewareMetrics")) {
            return;
        }
        ObjectNode page = (ObjectNode) root.with("pages").get("/monitor/middlewareMetrics");

        long total = middlewareInstanceRepository.count();
        long normal = middlewareInstanceRepository.countByStatus("正常");
        long alert = middlewareInstanceRepository.countByStatus("告警");

        ArrayNode stats = objectMapper.createArrayNode();
        ObjectNode s1 = objectMapper.createObjectNode();
        s1.put("label", "中间件总数"); s1.put("value", String.valueOf(total)); s1.put("hint", "DB/缓存/消息队列");
        stats.add(s1);
        ObjectNode s2 = objectMapper.createObjectNode();
        s2.put("label", "状态正常"); s2.put("value", String.valueOf(normal)); s2.put("hint", "运行正常");
        stats.add(s2);
        ObjectNode s3 = objectMapper.createObjectNode();
        s3.put("label", "告警中"); s3.put("value", String.valueOf(alert)); s3.put("hint", "需关注");
        stats.add(s3);
        ObjectNode s4 = objectMapper.createObjectNode();
        s4.put("label", "今日采集次数"); s4.put("value", String.valueOf(total * 720)); s4.put("hint", "2分钟一次");
        stats.add(s4);
        page.set("stats", stats);

        List<MiddlewareInstance> instances = middlewareInstanceRepository.findAll();
        ArrayNode rows = objectMapper.createArrayNode();
        for (MiddlewareInstance instance : instances) {
            ObjectNode node = objectMapper.createObjectNode();
            node.put("name", instance.getName());
            node.put("middlewareType", instance.getMiddlewareType());
            node.put("version", defaultText(instance.getVersion(), "-"));
            node.put("host", instance.getHost());
            node.put("port", instance.getPort());
            node.put("cpuUsage", defaultText(instance.getCpuUsage(), "-"));
            node.put("memUsage", defaultText(instance.getMemUsage(), "-"));
            node.put("connectCount", defaultText(instance.getConnectCount(), "-"));
            node.put("qps", defaultText(instance.getQps(), "-"));
            node.put("status", instance.getStatus());
            rows.add(node);
        }

        com.fasterxml.jackson.databind.JsonNode tables = page.get("tables");
        if (tables != null && tables.isArray() && tables.size() > 0) {
            ((ObjectNode) tables.get(0)).set("rows", rows);
        }
    }

    private void patchDataStoragePage(ObjectNode root) {
        if (!root.with("pages").has("/monitor/dataStorage")) {
            return;
        }
        ObjectNode page = (ObjectNode) root.with("pages").get("/monitor/dataStorage");

        long totalNodes = storageNodeRepository.count();
        long normalNodes = storageNodeRepository.countByStatus("正常");

        ArrayNode stats = objectMapper.createArrayNode();
        ObjectNode s1 = objectMapper.createObjectNode();
        s1.put("label", "存储节点总数"); s1.put("value", String.valueOf(totalNodes)); s1.put("hint", "ES/Prometheus/VictoriaMetrics");
        stats.add(s1);
        ObjectNode s2 = objectMapper.createObjectNode();
        s2.put("label", "正常节点"); s2.put("value", String.valueOf(normalNodes)); s2.put("hint", "运行正常");
        stats.add(s2);
        ObjectNode s3 = objectMapper.createObjectNode();
        s3.put("label", "今日写入量"); s3.put("value", "128GB"); s3.put("hint", "全部节点累计");
        stats.add(s3);
        ObjectNode s4 = objectMapper.createObjectNode();
        s4.put("label", "今日查询次数"); s4.put("value", "842,310"); s4.put("hint", "全部节点累计");
        stats.add(s4);
        page.set("stats", stats);

        List<StorageNode> nodes = storageNodeRepository.findAll();
        ArrayNode nodeRows = objectMapper.createArrayNode();
        for (StorageNode storageNode : nodes) {
            ObjectNode node = objectMapper.createObjectNode();
            node.put("node", storageNode.getNode());
            node.put("role", storageNode.getRole());
            node.put("used", defaultText(storageNode.getUsed(), "-"));
            node.put("total", defaultText(storageNode.getTotal(), "-"));
            node.put("writeRate", defaultText(storageNode.getWriteRate(), "-"));
            node.put("queryRate", defaultText(storageNode.getQueryRate(), "-"));
            node.put("status", storageNode.getStatus());
            nodeRows.add(node);
        }

        com.fasterxml.jackson.databind.JsonNode tables = page.get("tables");
        if (tables != null && tables.isArray() && tables.size() > 0) {
            ((ObjectNode) tables.get(0)).set("rows", nodeRows);
        }
    }

    private void patchDataGovernancePage(ObjectNode root) {
        if (!root.with("pages").has("/monitor/dataGovernance")) {
            return;
        }
        ObjectNode page = (ObjectNode) root.with("pages").get("/monitor/dataGovernance");

        long totalPolicies = archivePolicyRepository.count();
        long totalHistory = archiveHistoryRepository.count();

        ArrayNode stats = objectMapper.createArrayNode();
        ObjectNode s1 = objectMapper.createObjectNode();
        s1.put("label", "归档策略总数"); s1.put("value", String.valueOf(totalPolicies)); s1.put("hint", "已配置策略");
        stats.add(s1);
        ObjectNode s2 = objectMapper.createObjectNode();
        s2.put("label", "本月执行次数"); s2.put("value", String.valueOf(totalHistory)); s2.put("hint", "本月归档任务");
        stats.add(s2);
        ObjectNode s3 = objectMapper.createObjectNode();
        s3.put("label", "本月归档量"); s3.put("value", "116.6GB"); s3.put("hint", "累计归档数据");
        stats.add(s3);
        ObjectNode s4 = objectMapper.createObjectNode();
        s4.put("label", "节省存储空间"); s4.put("value", "47.2GB"); s4.put("hint", "压缩后节省");
        stats.add(s4);
        page.set("stats", stats);

        List<ArchivePolicy> policies = archivePolicyRepository.findAll();
        ArrayNode policyRows = objectMapper.createArrayNode();
        for (ArchivePolicy policy : policies) {
            ObjectNode node = objectMapper.createObjectNode();
            node.put("policyName", policy.getPolicyName());
            node.put("targetType", policy.getTargetType());
            node.put("hotDays", policy.getHotDays());
            node.put("warmDays", policy.getWarmDays());
            node.put("coldDays", policy.getColdDays());
            node.put("compress", policy.getCompress() == 1 ? "是" : "否");
            node.put("status", policy.getStatus());
            policyRows.add(node);
        }

        List<ArchiveHistory> histories = archiveHistoryRepository.findAll();
        ArrayNode historyRows = objectMapper.createArrayNode();
        for (ArchiveHistory history : histories) {
            ObjectNode node = objectMapper.createObjectNode();
            node.put("executedAt", history.getExecutedAt().format(DateFormats.MINUTE_PRECISION));
            node.put("policy", history.getPolicy());
            node.put("dataSize", defaultText(history.getDataSize(), "-"));
            node.put("duration", history.getDuration() != null ? history.getDuration() + "s" : "-");
            node.put("status", history.getStatus());
            historyRows.add(node);
        }

        com.fasterxml.jackson.databind.JsonNode tables = page.get("tables");
        if (tables != null && tables.isArray()) {
            if (tables.size() > 0) ((ObjectNode) tables.get(0)).set("rows", policyRows);
            if (tables.size() > 1) ((ObjectNode) tables.get(1)).set("rows", historyRows);
        }
    }

    private void patchNotifyChannelsPage(ObjectNode root) {
        if (!root.with("pages").has("/monitor/notifyChannels")) {
            return;
        }
        ObjectNode page = (ObjectNode) root.with("pages").get("/monitor/notifyChannels");

        long total = notifyChannelRepository.count();
        long normal = notifyChannelRepository.countByStatus("正常");
        long abnormal = notifyChannelRepository.countByStatus("异常");

        ArrayNode stats = objectMapper.createArrayNode();
        ObjectNode s1 = objectMapper.createObjectNode();
        s1.put("label", "通知渠道总数"); s1.put("value", String.valueOf(total)); s1.put("hint", "邮件/钉钉/微信/SMS");
        stats.add(s1);
        ObjectNode s2 = objectMapper.createObjectNode();
        s2.put("label", "正常渠道"); s2.put("value", String.valueOf(normal)); s2.put("hint", "可用");
        stats.add(s2);
        ObjectNode s3 = objectMapper.createObjectNode();
        s3.put("label", "异常渠道"); s3.put("value", String.valueOf(abnormal)); s3.put("hint", "需检查");
        stats.add(s3);
        ObjectNode s4 = objectMapper.createObjectNode();
        s4.put("label", "今日发送总数"); s4.put("value", "790"); s4.put("hint", "全渠道累计");
        stats.add(s4);
        page.set("stats", stats);

        List<NotifyChannel> channels = notifyChannelRepository.findAll();
        ArrayNode rows = objectMapper.createArrayNode();
        for (NotifyChannel channel : channels) {
            ObjectNode node = objectMapper.createObjectNode();
            node.put("channelName", channel.getChannelName());
            node.put("channelType", channel.getChannelType());
            node.put("config", defaultText(channel.getConfig(), "-"));
            node.put("sentToday", channel.getSentToday());
            node.put("failToday", channel.getFailToday());
            node.put("status", channel.getStatus());
            rows.add(node);
        }

        com.fasterxml.jackson.databind.JsonNode tables = page.get("tables");
        if (tables != null && tables.isArray() && tables.size() > 0) {
            ((ObjectNode) tables.get(0)).set("rows", rows);
        }
    }

    private void patchAlertHistoryPage(ObjectNode root) {
        if (!root.with("pages").has("/monitor/alertHistory")) {
            return;
        }
        ObjectNode page = (ObjectNode) root.with("pages").get("/monitor/alertHistory");

        long total = alertEventRepository.count();
        long active = alertEventRepository.countByStatus("处理中");
        long resolved = alertEventRepository.countByStatus("已恢复");

        ArrayNode stats = objectMapper.createArrayNode();
        ObjectNode s1 = objectMapper.createObjectNode();
        s1.put("label", "告警事件总数"); s1.put("value", String.valueOf(total)); s1.put("hint", "历史累计");
        stats.add(s1);
        ObjectNode s2 = objectMapper.createObjectNode();
        s2.put("label", "处理中"); s2.put("value", String.valueOf(active)); s2.put("hint", "待处理");
        stats.add(s2);
        ObjectNode s3 = objectMapper.createObjectNode();
        s3.put("label", "已恢复"); s3.put("value", String.valueOf(resolved)); s3.put("hint", "本日");
        stats.add(s3);
        ObjectNode s4 = objectMapper.createObjectNode();
        s4.put("label", "平均响应时长"); s4.put("value", "4.6min"); s4.put("hint", "本日平均");
        stats.add(s4);
        page.set("stats", stats);

        List<AlertEvent> events = alertEventRepository.findAll();
        ArrayNode rows = objectMapper.createArrayNode();
        for (AlertEvent event : events) {
            ObjectNode node = objectMapper.createObjectNode();
            node.put("triggeredAt", event.getTriggeredAt().format(DateFormats.MINUTE_PRECISION));
            node.put("ruleName", event.getRuleName());
            node.put("source", event.getSource());
            node.put("severity", event.getSeverity());
            node.put("status", event.getStatus());
            node.put("handler", defaultText(event.getHandler(), "-"));
            node.put("resolvedAt", event.getResolvedAt() != null
                ? event.getResolvedAt().format(DateFormats.MINUTE_PRECISION) : "-");
            rows.add(node);
        }

        com.fasterxml.jackson.databind.JsonNode tables = page.get("tables");
        if (tables != null && tables.isArray() && tables.size() > 0) {
            ((ObjectNode) tables.get(0)).set("rows", rows);
        }
    }

    private void patchOnCallMgmtPage(ObjectNode root) {
        if (!root.with("pages").has("/monitor/onCallMgmt")) {
            return;
        }
        ObjectNode page = (ObjectNode) root.with("pages").get("/monitor/onCallMgmt");

        ArrayNode stats = objectMapper.createArrayNode();
        ObjectNode s1 = objectMapper.createObjectNode();
        s1.put("label", "本周值班安排"); s1.put("value", String.valueOf(onCallScheduleRepository.count())); s1.put("hint", "已安排");
        stats.add(s1);
        ObjectNode s2 = objectMapper.createObjectNode();
        s2.put("label", "响应记录"); s2.put("value", String.valueOf(responseRecordRepository.count())); s2.put("hint", "历史响应");
        stats.add(s2);
        ObjectNode s3 = objectMapper.createObjectNode();
        s3.put("label", "平均响应时间"); s3.put("value", "4.1min"); s3.put("hint", "最近30天");
        stats.add(s3);
        ObjectNode s4 = objectMapper.createObjectNode();
        s4.put("label", "平均解决时间"); s4.put("value", "35.6min"); s4.put("hint", "最近30天");
        stats.add(s4);
        page.set("stats", stats);

        List<OnCallSchedule> schedules = onCallScheduleRepository.findAll();
        ArrayNode scheduleRows = objectMapper.createArrayNode();
        for (OnCallSchedule schedule : schedules) {
            ObjectNode node = objectMapper.createObjectNode();
            node.put("date", schedule.getDutyDate().toString());
            node.put("weekday", schedule.getWeekday());
            node.put("group", schedule.getGroupName());
            node.put("person", schedule.getPerson());
            node.put("shift", schedule.getShift());
            node.put("status", schedule.getStatus());
            scheduleRows.add(node);
        }

        List<ResponseRecord> records = responseRecordRepository.findAll();
        ArrayNode recordRows = objectMapper.createArrayNode();
        for (ResponseRecord record : records) {
            ObjectNode node = objectMapper.createObjectNode();
            node.put("alertName", record.getAlertName());
            node.put("triggeredAt", record.getTriggeredAt().format(DateFormats.MINUTE_PRECISION));
            node.put("responder", record.getResponder());
            node.put("responseMin", record.getResponseMin() != null ? record.getResponseMin() : 0);
            node.put("resolveMin", record.getResolveMin() != null ? record.getResolveMin() : 0);
            node.put("result", record.getResult());
            recordRows.add(node);
        }

        com.fasterxml.jackson.databind.JsonNode tables = page.get("tables");
        if (tables != null && tables.isArray()) {
            if (tables.size() > 0) ((ObjectNode) tables.get(0)).set("rows", scheduleRows);
            if (tables.size() > 1) ((ObjectNode) tables.get(1)).set("rows", recordRows);
        }
    }

    private void patchDashboardsPage(ObjectNode root) {
        if (!root.with("pages").has("/monitor/dashboards")) {
            return;
        }
        ObjectNode page = (ObjectNode) root.with("pages").get("/monitor/dashboards");

        long total = dashboardRepository.count();

        ArrayNode stats = objectMapper.createArrayNode();
        ObjectNode s1 = objectMapper.createObjectNode();
        s1.put("label", "看板总数"); s1.put("value", String.valueOf(total)); s1.put("hint", "全部看板");
        stats.add(s1);
        ObjectNode s2 = objectMapper.createObjectNode();
        s2.put("label", "今日访问量"); s2.put("value", "325"); s2.put("hint", "PV");
        stats.add(s2);
        ObjectNode s3 = objectMapper.createObjectNode();
        s3.put("label", "共享看板"); s3.put("value", "5"); s3.put("hint", "团队共享");
        stats.add(s3);
        ObjectNode s4 = objectMapper.createObjectNode();
        s4.put("label", "图表总数"); s4.put("value", "87"); s4.put("hint", "全部图表");
        stats.add(s4);
        page.set("stats", stats);

        List<Dashboard> dashboards = dashboardRepository.findAll();
        ArrayNode rows = objectMapper.createArrayNode();
        for (Dashboard dashboard : dashboards) {
            ObjectNode node = objectMapper.createObjectNode();
            node.put("name", dashboard.getName());
            node.put("category", dashboard.getCategory());
            node.put("charts", dashboard.getCharts());
            node.put("creator", dashboard.getCreator());
            node.put("shared", dashboard.getShared() == 1 ? "是" : "否");
            node.put("visitToday", dashboard.getVisitToday());
            node.put("updatedAt", dashboard.getUpdatedAt().format(DateFormats.MINUTE_PRECISION));
            rows.add(node);
        }

        com.fasterxml.jackson.databind.JsonNode tables = page.get("tables");
        if (tables != null && tables.isArray() && tables.size() > 0) {
            ((ObjectNode) tables.get(0)).set("rows", rows);
        }
    }

    private void patchTopologyPage(ObjectNode root) {
        if (!root.with("pages").has("/monitor/topology")) {
            return;
        }
        ObjectNode page = (ObjectNode) root.with("pages").get("/monitor/topology");

        long total = topologyItemRepository.count();
        long abnormalLinks = topologyLinkRepository.countByStatus("告警");

        ArrayNode stats = objectMapper.createArrayNode();
        ObjectNode s1 = objectMapper.createObjectNode();
        s1.put("label", "拓扑图总数"); s1.put("value", String.valueOf(total)); s1.put("hint", "全部拓扑");
        stats.add(s1);
        ObjectNode s2 = objectMapper.createObjectNode();
        s2.put("label", "异常链路"); s2.put("value", String.valueOf(abnormalLinks)); s2.put("hint", "需关注");
        stats.add(s2);
        ObjectNode s3 = objectMapper.createObjectNode();
        s3.put("label", "节点总数"); s3.put("value", String.valueOf(topologyLinkRepository.count())); s3.put("hint", "全部节点");
        stats.add(s3);
        ObjectNode s4 = objectMapper.createObjectNode();
        s4.put("label", "今日刷新次数"); s4.put("value", "1,248"); s4.put("hint", "自动刷新");
        stats.add(s4);
        page.set("stats", stats);

        List<TopologyItem> items = topologyItemRepository.findAll();
        ArrayNode itemRows = objectMapper.createArrayNode();
        for (TopologyItem item : items) {
            ObjectNode node = objectMapper.createObjectNode();
            node.put("name", item.getName());
            node.put("type", item.getType());
            node.put("nodes", item.getNodeCount());
            node.put("links", item.getLinkCount());
            node.put("abnormal", item.getAbnormal());
            node.put("autoRefresh", item.getAutoRefresh() == 1 ? "是" : "否");
            node.put("updatedAt", item.getUpdatedAt().format(DateFormats.MINUTE_PRECISION));
            itemRows.add(node);
        }

        List<TopologyLink> links = topologyLinkRepository.findAll();
        ArrayNode linkRows = objectMapper.createArrayNode();
        for (TopologyLink link : links) {
            if ("告警".equals(link.getStatus())) {
                ObjectNode node = objectMapper.createObjectNode();
                node.put("sourceNode", link.getSourceNode());
                node.put("targetNode", link.getTargetNode());
                node.put("linkType", link.getLinkType());
                node.put("latency", defaultText(link.getLatency(), "-"));
                node.put("errorRate", defaultText(link.getErrorRate(), "-"));
                node.put("status", link.getStatus());
                linkRows.add(node);
            }
        }

        com.fasterxml.jackson.databind.JsonNode tables = page.get("tables");
        if (tables != null && tables.isArray()) {
            if (tables.size() > 0) ((ObjectNode) tables.get(0)).set("rows", itemRows);
            if (tables.size() > 1) ((ObjectNode) tables.get(1)).set("rows", linkRows);
        }
    }

    private void patchChartsPage(ObjectNode root) {
        if (!root.with("pages").has("/monitor/charts")) {
            return;
        }
        ObjectNode page = (ObjectNode) root.with("pages").get("/monitor/charts");

        long totalScreens = bigScreenRepository.count();
        long totalTemplates = chartTemplateRepository.count();

        ArrayNode stats = objectMapper.createArrayNode();
        ObjectNode s1 = objectMapper.createObjectNode();
        s1.put("label", "大屏总数"); s1.put("value", String.valueOf(totalScreens)); s1.put("hint", "全部大屏");
        stats.add(s1);
        ObjectNode s2 = objectMapper.createObjectNode();
        s2.put("label", "图表模板"); s2.put("value", String.valueOf(totalTemplates)); s2.put("hint", "可复用模板");
        stats.add(s2);
        ObjectNode s3 = objectMapper.createObjectNode();
        s3.put("label", "今日访问量"); s3.put("value", "186"); s3.put("hint", "大屏访问PV");
        stats.add(s3);
        ObjectNode s4 = objectMapper.createObjectNode();
        s4.put("label", "共享大屏"); s4.put("value", "4"); s4.put("hint", "团队共享");
        stats.add(s4);
        page.set("stats", stats);

        List<BigScreen> screens = bigScreenRepository.findAll();
        ArrayNode screenRows = objectMapper.createArrayNode();
        for (BigScreen screen : screens) {
            ObjectNode node = objectMapper.createObjectNode();
            node.put("name", screen.getName());
            node.put("resolution", defaultText(screen.getResolution(), "-"));
            node.put("charts", screen.getCharts());
            node.put("autoPlay", screen.getAutoPlay() == 1 ? "是" : "否");
            node.put("shared", screen.getShared() == 1 ? "是" : "否");
            node.put("visitToday", screen.getVisitToday());
            screenRows.add(node);
        }

        List<ChartTemplate> templates = chartTemplateRepository.findAll();
        ArrayNode templateRows = objectMapper.createArrayNode();
        for (ChartTemplate template : templates) {
            ObjectNode node = objectMapper.createObjectNode();
            node.put("templateName", template.getTemplateName());
            node.put("chartType", template.getChartType());
            node.put("category", template.getCategory());
            node.put("usedCount", template.getUsedCount());
            templateRows.add(node);
        }

        com.fasterxml.jackson.databind.JsonNode tables = page.get("tables");
        if (tables != null && tables.isArray()) {
            if (tables.size() > 0) ((ObjectNode) tables.get(0)).set("rows", screenRows);
            if (tables.size() > 1) ((ObjectNode) tables.get(1)).set("rows", templateRows);
        }
    }

    private void patchMonitorOverview(ObjectNode root) {
        if (!root.with("pages").has("/")) {
            return;
        }
        ObjectNode page = (ObjectNode) root.with("pages").get("/");
        long totalDevices = networkCollectionService.countDevices();
        long normalDevices = networkCollectionService.countDevicesByStatus("采集正常");
        long totalTemplates = networkCollectionService.countTemplates();
        long discoveredDevices = networkCollectionService.countDevicesByStatus("已发现");

        ArrayNode stats = objectMapper.createArrayNode();
        ObjectNode s1 = objectMapper.createObjectNode();
        s1.put("label", "网络设备总数");
        s1.put("value", String.valueOf(totalDevices));
        s1.put("hint", "路由器 / 交换机 / 防火墙");
        stats.add(s1);

        ObjectNode s2 = objectMapper.createObjectNode();
        s2.put("label", "采集正常");
        s2.put("value", String.valueOf(normalDevices));
        s2.put("hint", "SNMP/SSH 采集成功");
        stats.add(s2);

        ObjectNode s3 = objectMapper.createObjectNode();
        s3.put("label", "采集模板");
        s3.put("value", String.valueOf(totalTemplates));
        s3.put("hint", "自定义采集指标集");
        stats.add(s3);

        ObjectNode s4 = objectMapper.createObjectNode();
        s4.put("label", "待纳管设备");
        s4.put("value", String.valueOf(discoveredDevices));
        s4.put("hint", "自动发现未录入");
        stats.add(s4);

        page.set("stats", stats);

        List<MetricSnapshot> recentMetrics = networkCollectionService.getRecentMetrics();
        ArrayNode rows = objectMapper.createArrayNode();
        for (MetricSnapshot snapshot : recentMetrics) {
            ObjectNode node = objectMapper.createObjectNode();
            node.put("device", snapshot.getDeviceCode());
            node.put("metric", snapshot.getMetricName());
            node.put("value", snapshot.getMetricValue());
            node.put("time", snapshot.getCollectedAt() != null ? snapshot.getCollectedAt().format(DateFormats.SECOND_PRECISION) : "-");
            rows.add(node);
        }
        if (root.with("pages").get("/").has("tables")) {
            com.fasterxml.jackson.databind.JsonNode tables = root.with("pages").get("/").get("tables");
            if (tables.isArray() && tables.size() > 0) {
                ((ObjectNode) tables.get(0)).set("rows", rows);
            }
        }
    }

    private void patchNetworkDevices(ObjectNode root) {
        if (!root.with("pages").has("/collection/networkDevices")) {
            return;
        }
        ObjectNode page = (ObjectNode) root.with("pages").get("/collection/networkDevices");
        List<NetworkDevice> devices = networkCollectionService.listDevices();
        ArrayNode rows = objectMapper.createArrayNode();
        for (NetworkDevice device : devices) {
            ObjectNode node = objectMapper.createObjectNode();
            node.put("id", device.getId());
            node.put("deviceCode", device.getDeviceCode());
            node.put("name", device.getName());
            node.put("ip", device.getIp());
            node.put("deviceType", device.getDeviceType());
            node.put("vendor", device.getVendor());
            node.put("protocol", device.getProtocol());
            node.put("port", device.getPort());
            node.put("status", device.getStatus());
            node.put("lastCollectedAt", device.getLastCollectedAt() != null
                ? device.getLastCollectedAt().format(DateFormats.MINUTE_PRECISION) : "-");
            node.put("description", defaultText(device.getDescription(), "-"));
            rows.add(node);
        }
        page.set("rows", rows);

        ArrayNode tabs = objectMapper.createArrayNode();
        tabs.add("全部设备(" + devices.size() + ")");
        tabs.add("路由器(" + devices.stream().filter(d -> "路由器".equals(d.getDeviceType())).count() + ")");
        tabs.add("交换机(" + devices.stream().filter(d -> "交换机".equals(d.getDeviceType())).count() + ")");
        tabs.add("防火墙(" + devices.stream().filter(d -> "防火墙".equals(d.getDeviceType())).count() + ")");
        tabs.add("采集正常(" + networkCollectionService.countDevicesByStatus("采集正常") + ")");
        tabs.add("采集异常(" + networkCollectionService.countDevicesByStatus("采集异常") + ")");
        page.set("tabs", tabs);
    }

    private void patchCollectionTemplates(ObjectNode root) {
        if (!root.with("pages").has("/collection/templates")) {
            return;
        }
        ObjectNode page = (ObjectNode) root.with("pages").get("/collection/templates");
        List<CollectionTemplate> templates = networkCollectionService.listTemplates();
        ArrayNode rows = objectMapper.createArrayNode();
        for (CollectionTemplate template : templates) {
            ObjectNode node = objectMapper.createObjectNode();
            node.put("id", template.getId());
            node.put("templateCode", template.getTemplateCode());
            node.put("name", template.getName());
            node.put("deviceType", template.getDeviceType());
            node.put("protocol", template.getProtocol());
            node.put("intervalSeconds", template.getIntervalSeconds());
            node.put("interval", template.getIntervalSeconds() + "s");
            node.put("enabled", Boolean.TRUE.equals(template.getEnabled()) ? "已启用" : "已停用");
            node.put("description", defaultText(template.getDescription(), "-"));
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
        if ("monitor".equals(platformKey)) {
            return "监控管理";
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
