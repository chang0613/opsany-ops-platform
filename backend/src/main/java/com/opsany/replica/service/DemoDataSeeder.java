package com.opsany.replica.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.opsany.replica.config.AppProperties;
import com.opsany.replica.domain.AppUser;
import com.opsany.replica.domain.AlertEvent;
import com.opsany.replica.domain.AlertRule;
import com.opsany.replica.domain.ArchiveHistory;
import com.opsany.replica.domain.ArchivePolicy;
import com.opsany.replica.domain.BigScreen;
import com.opsany.replica.domain.ChartTemplate;
import com.opsany.replica.domain.CollectionTemplate;
import com.opsany.replica.domain.Dashboard;
import com.opsany.replica.domain.DutyGroup;
import com.opsany.replica.domain.InfraServer;
import com.opsany.replica.domain.K8sCluster;
import com.opsany.replica.domain.K8sPod;
import com.opsany.replica.domain.LogCollector;
import com.opsany.replica.domain.MetricSnapshot;
import com.opsany.replica.domain.MiddlewareInstance;
import com.opsany.replica.domain.NotifyChannel;
import com.opsany.replica.domain.OnCallSchedule;
import com.opsany.replica.domain.ResponseRecord;
import com.opsany.replica.domain.ServiceProbe;
import com.opsany.replica.domain.StorageNode;
import com.opsany.replica.domain.TopologyItem;
import com.opsany.replica.domain.TopologyLink;
import com.opsany.replica.domain.DutyShift;
import com.opsany.replica.domain.MenuPermission;
import com.opsany.replica.domain.MessageSubscription;
import com.opsany.replica.domain.NetworkDevice;
import com.opsany.replica.domain.NotificationMessage;
import com.opsany.replica.domain.PlatformNavigationGroup;
import com.opsany.replica.domain.PlatformNavigationItem;
import com.opsany.replica.domain.TaskRecord;
import com.opsany.replica.domain.WorkOrder;
import com.opsany.replica.domain.WorkOrderCatalog;
import com.opsany.replica.domain.WorkOrderProcessDefinition;
import com.opsany.replica.domain.WorkOrderProcessNode;
import com.opsany.replica.repository.AlertEventRepository;
import com.opsany.replica.repository.AppUserRepository;
import com.opsany.replica.repository.AlertRuleRepository;
import com.opsany.replica.repository.ArchiveHistoryRepository;
import com.opsany.replica.repository.ArchivePolicyRepository;
import com.opsany.replica.repository.BigScreenRepository;
import com.opsany.replica.repository.ChartTemplateRepository;
import com.opsany.replica.repository.CollectionTemplateRepository;
import com.opsany.replica.repository.DashboardRepository;
import com.opsany.replica.repository.DutyScheduleRepository;
import com.opsany.replica.repository.InfraServerRepository;
import com.opsany.replica.repository.K8sClusterRepository;
import com.opsany.replica.repository.K8sPodRepository;
import com.opsany.replica.repository.LogCollectorRepository;
import com.opsany.replica.repository.MetricSnapshotRepository;
import com.opsany.replica.repository.MiddlewareInstanceRepository;
import com.opsany.replica.repository.NotifyChannelRepository;
import com.opsany.replica.repository.OnCallScheduleRepository;
import com.opsany.replica.repository.ResponseRecordRepository;
import com.opsany.replica.repository.ServiceProbeRepository;
import com.opsany.replica.repository.StorageNodeRepository;
import com.opsany.replica.repository.TopologyItemRepository;
import com.opsany.replica.repository.TopologyLinkRepository;
import com.opsany.replica.repository.MenuRepository;
import com.opsany.replica.repository.MessageSubscriptionRepository;
import com.opsany.replica.repository.NetworkDeviceRepository;
import com.opsany.replica.repository.NotificationMessageRepository;
import com.opsany.replica.repository.PlatformNavigationRepository;
import com.opsany.replica.repository.RoleRepository;
import com.opsany.replica.repository.TaskRecordRepository;
import com.opsany.replica.repository.WorkOrderProcessRepository;
import com.opsany.replica.repository.WorkOrderCatalogRepository;
import com.opsany.replica.repository.WorkOrderRepository;
import com.opsany.replica.security.PasswordCodec;

import lombok.RequiredArgsConstructor;

@Component
@Order(10)
@RequiredArgsConstructor
public class DemoDataSeeder implements ApplicationRunner {

    private final AppProperties appProperties;
    private final PlatformTemplateService platformTemplateService;
    private final AppUserRepository appUserRepository;
    private final RoleRepository roleRepository;
    private final MenuRepository menuRepository;
    private final WorkOrderRepository workOrderRepository;
    private final TaskRecordRepository taskRecordRepository;
    private final NotificationMessageRepository notificationMessageRepository;
    private final MessageSubscriptionRepository messageSubscriptionRepository;
    private final DutyScheduleRepository dutyScheduleRepository;
    private final PlatformNavigationRepository platformNavigationRepository;
    private final WorkOrderProcessRepository workOrderProcessRepository;
    private final WorkOrderCatalogRepository workOrderCatalogRepository;
    private final NetworkDeviceRepository networkDeviceRepository;
    private final CollectionTemplateRepository collectionTemplateRepository;
    private final MetricSnapshotRepository metricSnapshotRepository;
    private final AlertRuleRepository alertRuleRepository;
    private final ServiceProbeRepository serviceProbeRepository;
    private final LogCollectorRepository logCollectorRepository;
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
    private final PasswordCodec passwordCodec;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!appProperties.getSeed().isEnabled()) {
            return;
        }

        seedRoles();
        seedMenus();
        seedNavigations();
        seedProcesses();
        seedCatalogs();

        AppUser demoUser = ensureUser("demo", "演示用户", "123456.coM", Arrays.asList("PLATFORM_ADMIN", "REQUESTER"));
        ensureUser("admin", "管理员", "123456.coM", Arrays.asList("PLATFORM_ADMIN", "ENGINEER"));

        seedSubscriptions(demoUser.getUsername());
        seedNavigationFavorites(demoUser.getId());
        seedDuty();
        seedWorkOrders(demoUser);
        seedTasks();
        seedMessages();
        seedNetworkDevices();
        seedCollectionTemplates();
        seedMetricSnapshots();
        seedAlertRules();
        seedServiceProbes();
        seedLogCollectors();
        seedInfraServers();
        seedK8sClusters();
        seedK8sPods();
        seedMiddlewareInstances();
        seedStorageNodes();
        seedArchivePolicies();
        seedArchiveHistory();
        seedNotifyChannels();
        seedAlertEvents();
        seedOnCallSchedules();
        seedResponseRecords();
        seedDashboards();
        seedTopologyItems();
        seedTopologyLinks();
        seedBigScreens();
        seedChartTemplates();
    }

    private AppUser ensureUser(String username, String displayName, String password, List<String> roleCodes) {
        AppUser existing = appUserRepository.findByUsername(username);
        if (existing == null) {
            existing = AppUser.builder()
                .username(username)
                .displayName(displayName)
                .passwordHash(passwordCodec.encode(password))
                .status("ACTIVE")
                .createdAt(LocalDateTime.now())
                .enabled(true)
                .build();
            appUserRepository.insert(existing);
        }

        for (String roleCode : roleCodes) {
            if (roleRepository.countUserRole(existing.getId(), roleCode) == 0) {
                roleRepository.bindUserRole(existing.getId(), roleCode);
            }
        }
        return existing;
    }

    private void seedRoles() {
        ensureRole("PLATFORM_ADMIN", "平台管理员", "拥有平台全部菜单和工单操作权限", 1);
        ensureRole("ENGINEER", "运维工程师", "处理工单、值班和告警消息", 2);
        ensureRole("REQUESTER", "提单用户", "提交工单并确认处理结果", 3);
    }

    private void ensureRole(String roleCode, String roleName, String description, int sortNo) {
        if (roleRepository.countByRoleCode(roleCode) == 0) {
            roleRepository.insertRole(roleCode, roleName, description, sortNo);
        }
    }

    private void seedMenus() {
        List<MenuPermission> menus = Arrays.asList(
            menu("WORKBENCH_OVERVIEW", "工作台", 1, "概览", "/", "概", 1, "workbench:view"),
            menu("SERVICE_PORTAL", "运维中心", 2, "服务门户", "/personSetting/serviceFolder", "服", 1, "portal:view"),
            menu("WORK_ORDER_MANAGE", "运维中心", 2, "工单管理", "/personSetting/orderManage", "工", 2, "order:view"),
            menu("TASK_MANAGE", "运维中心", 2, "任务管理", "/personSetting/taskManage", "任", 3, "task:view"),
            menu("MY_DUTY", "运维中心", 2, "我的值班", "/personSetting/myDuty", "值", 4, "duty:view"),
            menu("BIG_SCREEN", "运维中心", 2, "大屏展示", "/bigScreen/bigScreenList", "大", 5, "screen:view"),
            menu("MESSAGE_MANAGE", "消息中心", 3, "消息管理", "/msgCenter/messageManage", "消", 1, "message:view"),
            menu("SUBSCRIPTION_SETTING", "消息中心", 3, "订阅设置", "/msgCenter/subscriptionSetting", "订", 2, "subscription:edit"),
            menu("ORDER_DIRECTORY", "流程管理", 4, "工单目录", "/serveSetting/orderDirectory", "目", 1, "catalog:view"),
            menu("ORDER_PROCESS", "流程管理", 4, "工单流程", "/serveSetting/orderProcess", "流", 2, "process:view"),
            menu("SLA_SETTING", "流程管理", 4, "SLA管理", "/serveSetting/slaSetting", "S", 3, "sla:view"),
            menu("API_MANAGE", "流程管理", 4, "API管理", "/serveSetting/apiManage", "A", 4, "api:view"),
            menu("DUTY_MANAGE", "流程管理", 4, "值班管理", "/serveSetting/dutyManage", "班", 5, "duty:manage"),
            menu("NAV_SETTING", "平台设置", 5, "导航管理", "/serveSetting/navSetting", "导", 1, "nav:view"),
            menu("SYSTEM_SETTING", "平台设置", 5, "系统设置", "/serveSetting/systemSetting", "系", 2, "system:view"),
            menu("MONITOR_DATA_COLLECTION", "监控管理", 6, "监控数据采集", "", "监", 1, "monitor:view"),
            menu("NETWORK_DEVICE_METRICS", "监控管理", 6, "网络设备指标采集", "/monitor/networkDevices", "网", 2, "monitor:view"),
            menu("SERVICE_PROBE", "监控管理", 6, "全链路服务探测", "/monitor/serviceProbe", "探", 3, "monitor:view"),
            menu("INFRA_METRICS", "监控管理", 6, "硬件与基础设施采集", "/monitor/infraMetrics", "硬", 4, "monitor:view"),
            menu("CONTAINER_METRICS", "监控管理", 6, "容器与K8s采集", "/monitor/containerMetrics", "容", 5, "monitor:view"),
            menu("MIDDLEWARE_METRICS", "监控管理", 6, "数据库与中间件采集", "/monitor/middlewareMetrics", "库", 6, "monitor:view"),
            menu("LOG_COLLECTION", "监控管理", 6, "日志采集管理", "/monitor/logCollection", "日", 7, "monitor:view"),
            menu("MONITOR_DATA_STORAGE", "监控管理", 6, "数据存储与处理", "", "存", 8, "monitor:view"),
            menu("ALERT_HISTORY_STORAGE", "监控管理", 6, "告警历史与时序数据", "/monitor/dataStorage", "时", 9, "monitor:view"),
            menu("DATA_GOVERNANCE", "监控管理", 6, "数据归档与治理", "/monitor/dataGovernance", "归", 10, "monitor:view"),
            menu("MONITOR_ALERT", "监控管理", 6, "告警与通知", "", "警", 11, "monitor:view"),
            menu("ALERT_RULES", "监控管理", 6, "告警规则管理", "/monitor/alertRules", "规", 12, "monitor:view"),
            menu("NOTIFY_CHANNELS", "监控管理", 6, "通知渠道配置", "/monitor/notifyChannels", "通", 13, "monitor:view"),
            menu("ALERT_HISTORY", "监控管理", 6, "告警历史查询", "/monitor/alertHistory", "历", 14, "monitor:view"),
            menu("ONCALL_MGMT", "监控管理", 6, "值班与响应管理", "/monitor/onCallMgmt", "值", 15, "monitor:view"),
            menu("MONITOR_VIZ", "监控管理", 6, "可视化", "", "视", 16, "monitor:view"),
            menu("DASHBOARDS", "监控管理", 6, "看板管理", "/monitor/dashboards", "看", 17, "monitor:view"),
            menu("TOPOLOGY_MGMT", "监控管理", 6, "拓扑图管理", "/monitor/topology", "拓", 18, "monitor:view"),
            menu("CHARTS_SCREEN", "监控管理", 6, "图表与大屏", "/monitor/charts", "图", 19, "monitor:view")
        );

        for (MenuPermission menu : menus) {
            if (menuRepository.countByMenuCode(menu.getMenuCode()) == 0) {
                menuRepository.insert(menu);
            }
            if (menuRepository.countRoleMenu("PLATFORM_ADMIN", menu.getMenuCode()) == 0) {
                menuRepository.grantRoleMenu("PLATFORM_ADMIN", menu.getMenuCode());
            }
        }

        grantRequesterMenu("WORKBENCH_OVERVIEW");
        grantRequesterMenu("SERVICE_PORTAL");
        grantRequesterMenu("WORK_ORDER_MANAGE");
        grantRequesterMenu("TASK_MANAGE");
        grantRequesterMenu("MY_DUTY");
        grantRequesterMenu("MESSAGE_MANAGE");
        grantRequesterMenu("SUBSCRIPTION_SETTING");
    }

    private void grantRequesterMenu(String menuCode) {
        if (menuRepository.countRoleMenu("REQUESTER", menuCode) == 0) {
            menuRepository.grantRoleMenu("REQUESTER", menuCode);
        }
        if (menuRepository.countRoleMenu("ENGINEER", menuCode) == 0) {
            menuRepository.grantRoleMenu("ENGINEER", menuCode);
        }
    }

    private MenuPermission menu(
        String menuCode,
        String groupName,
        int groupSortNo,
        String label,
        String route,
        String icon,
        int sortNo,
        String permissionCode
    ) {
        return MenuPermission.builder()
            .menuCode(menuCode)
            .groupName(groupName)
            .groupSortNo(groupSortNo)
            .label(label)
            .route(route)
            .icon(icon)
            .sortNo(sortNo)
            .permissionCode(permissionCode)
            .visible(true)
            .build();
    }

    private void seedNavigations() {
        ObjectNode template = platformTemplateService.copyTemplate();
        ArrayNode groups = (ArrayNode) template.withArray("navigationGroups");
        if (platformNavigationRepository.countGroups() == 0) {
            for (int index = 0; index < groups.size(); index++) {
                JsonNode group = groups.get(index);
                platformNavigationRepository.insertGroup(PlatformNavigationGroup.builder()
                    .groupCode("GROUP_" + (index + 1))
                    .title(group.path("title").asText("未命名分组"))
                    .sortNo(index + 1)
                    .build());
            }
        }

        if (platformNavigationRepository.countItems() > 0) {
            return;
        }

        for (int groupIndex = 0; groupIndex < groups.size(); groupIndex++) {
            JsonNode group = groups.get(groupIndex);
            ArrayNode rows = (ArrayNode) group.withArray("rows");
            String groupCode = "GROUP_" + (groupIndex + 1);
            for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
                JsonNode row = rows.get(rowIndex);
                String creatorUsername = row.path("creator").asText("admin");
                String creatorDisplayName = "admin".equalsIgnoreCase(creatorUsername) ? "管理员" : creatorUsername;
                String name = row.path("name").asText("未命名导航");
                platformNavigationRepository.insertItem(PlatformNavigationItem.builder()
                    .itemCode("ITEM_" + row.path("id").asInt(groupIndex * 100 + rowIndex + 1))
                    .groupCode(groupCode)
                    .name(name)
                    .icon(defaultIfBlank(row.path("icon").asText(), name.substring(0, 1)))
                    .creatorUsername(creatorUsername)
                    .creatorDisplayName(creatorDisplayName)
                    .link(row.path("link").asText("/"))
                    .mobileVisible("是".equals(row.path("mobile").asText()))
                    .description(defaultIfBlank(row.path("desc").asText(), "平台导航入口"))
                    .sortNo(rowIndex + 1)
                    .enabled(true)
                    .build());
            }
        }
    }

    private void seedNavigationFavorites(Long userId) {
        if (userId == null) {
            return;
        }
        ensureNavigationFavorite(userId, "ITEM_1", 1);
        ensureNavigationFavorite(userId, "ITEM_3", 2);
        ensureNavigationFavorite(userId, "ITEM_4", 3);
    }

    private void ensureNavigationFavorite(Long userId, String itemCode, int sortNo) {
        if (platformNavigationRepository.countFavorite(userId, itemCode) > 0) {
            return;
        }
        if (platformNavigationRepository.countByItemCode(itemCode) == 0) {
            return;
        }
        platformNavigationRepository.insertFavorite(userId, itemCode, sortNo);
    }

    private void seedProcesses() {
        if (workOrderProcessRepository.countByProcessCode(WorkOrderProcessService.DEFAULT_PROCESS_CODE) == 0) {
            workOrderProcessRepository.insertDefinition(WorkOrderProcessDefinition.builder()
                .processCode(WorkOrderProcessService.DEFAULT_PROCESS_CODE)
                .name("标准服务请求流程")
                .category("请求管理")
                .version(1)
                .status("已发布")
                .owner("管理员")
                .creator("管理员")
                .updater("管理员")
                .updatedAt(LocalDateTime.now())
                .description("适用于提单、分派、工程师处理、申请人确认的标准流程")
                .definitionJson("{\"nodes\":[\"START\",\"TRIAGE\",\"ENGINEER_HANDLE\",\"REQUESTER_CONFIRM\",\"END_DONE\",\"END_REJECTED\"]}")
                .build());
        }

        if (workOrderProcessRepository.countNodesByProcessCode(WorkOrderProcessService.DEFAULT_PROCESS_CODE) > 0) {
            return;
        }

        workOrderProcessRepository.insertNode(node("START", "提交", "START", 1, "REQUESTER", "TRIAGE", null));
        workOrderProcessRepository.insertNode(node("TRIAGE", "主管受理", "APPROVAL", 2, "PLATFORM_ADMIN", "ENGINEER_HANDLE", "END_REJECTED"));
        workOrderProcessRepository.insertNode(node("ENGINEER_HANDLE", "工程师处理", "TASK", 3, "ENGINEER", "REQUESTER_CONFIRM", "END_REJECTED"));
        workOrderProcessRepository.insertNode(node("REQUESTER_CONFIRM", "申请人确认", "CONFIRM", 4, "REQUESTER", "END_DONE", "ENGINEER_HANDLE"));
        workOrderProcessRepository.insertNode(node("END_DONE", "已完成", "END", 5, "END", null, null));
        workOrderProcessRepository.insertNode(node("END_REJECTED", "已驳回", "END", 6, "END", null, null));
    }

    private void seedCatalogs() {
        if (workOrderCatalogRepository.count() > 0) {
            return;
        }
        ObjectNode template = platformTemplateService.copyTemplate();
        ObjectNode page = (ObjectNode) template.with("pages").get("/serveSetting/orderDirectory");
        ArrayNode rows = page.withArray("rows");
        for (int index = 0; index < rows.size(); index++) {
            JsonNode row = rows.get(index);
            seedCatalogRow(row, index);
        }
    }

    private void seedCatalogRow(JsonNode row, int index) {
        String ownerDisplayName = row.path("owner").asText("管理员");
        String ownerUsername = "管理员".equals(ownerDisplayName) ? "admin" : "demo";
        WorkOrderCatalog catalog = WorkOrderCatalog.builder()
            .catalogCode("CATALOG_" + (index + 1))
            .name(row.path("name").asText("未命名目录"))
            .category(defaultIfBlank(row.path("type").asText(), "默认分组"))
            .type(defaultIfBlank(row.path("type").asText(), "请求管理"))
            .scope(defaultIfBlank(row.path("scope").asText(), "全部用户"))
            .online(!row.path("online").asText("").contains("未"))
            .processCode(WorkOrderProcessService.DEFAULT_PROCESS_CODE)
            .slaName(defaultIfBlank(row.path("sla").asText(), "-"))
            .ownerUsername(ownerUsername)
            .ownerDisplayName(ownerDisplayName)
            .description(defaultIfBlank(row.path("desc").asText(), "-"))
            .sortNo(index + 1)
            .createdAt(DateFormats.parseFlexible(row.path("createdAt").asText()))
            .updatedAt(DateFormats.parseFlexible(row.path("createdAt").asText()))
            .build();
        workOrderCatalogRepository.insert(catalog);
    }

    private WorkOrderProcessNode node(
        String nodeCode,
        String nodeName,
        String nodeType,
        int sortNo,
        String assigneeRole,
        String nextApproveNode,
        String nextRejectNode
    ) {
        return WorkOrderProcessNode.builder()
            .processCode(WorkOrderProcessService.DEFAULT_PROCESS_CODE)
            .nodeCode(nodeCode)
            .nodeName(nodeName)
            .nodeType(nodeType)
            .sortNo(sortNo)
            .assigneeRole(assigneeRole)
            .nextApproveNode(nextApproveNode)
            .nextRejectNode(nextRejectNode)
            .build();
    }

    private void seedSubscriptions(String username) {
        if (!messageSubscriptionRepository.findByUsername(username).isEmpty()) {
            return;
        }

        List<MessageSubscription> defaults = Arrays.asList(
            subscription(username, "工单状态", "工单流程", true, false, true, true, false),
            subscription(username, "工单待办", "工单流程", true, false, true, true, false),
            subscription(username, "流程发布", "流程管理", true, false, true, false, false),
            subscription(username, "值班提醒", "值班管理", true, true, true, true, true),
            subscription(username, "告警通知", "事件中心", true, true, true, true, true)
        );

        for (MessageSubscription subscription : defaults) {
            messageSubscriptionRepository.insert(subscription);
        }
    }

    private MessageSubscription subscription(
        String username,
        String messageType,
        String source,
        boolean siteEnabled,
        boolean smsEnabled,
        boolean mailEnabled,
        boolean wxEnabled,
        boolean dingEnabled
    ) {
        return MessageSubscription.builder()
            .username(username)
            .messageType(messageType)
            .source(source)
            .siteEnabled(siteEnabled)
            .smsEnabled(smsEnabled)
            .mailEnabled(mailEnabled)
            .wxEnabled(wxEnabled)
            .dingEnabled(dingEnabled)
            .updatedAt(LocalDateTime.now())
            .build();
    }

    private void seedDuty() {
        DutyGroup dbGroup = ensureDutyGroup("数据库组", "admin", "管理员", 4, "7 x 24", "数据库值班组");
        DutyGroup appGroup = ensureDutyGroup("应用运维组", "demo", "演示用户", 6, "工作日", "应用运维值班组");
        DutyGroup infraGroup = ensureDutyGroup("基础设施组", "admin", "管理员", 5, "7 x 24", "基础设施值班组");

        seedShift(dbGroup, LocalDate.now().minusDays(1), "昨日值班", "00:00 - 08:00", "管理员", "admin", "已完成");
        seedShift(appGroup, LocalDate.now(), "今日值班", "09:00 - 18:00", "演示用户", "demo", "今日当班");
        seedShift(infraGroup, LocalDate.now().plusDays(1), "明日值班", "00:00 - 08:00", "管理员", "admin", "待值班");
    }

    private DutyGroup ensureDutyGroup(
        String name,
        String ownerUsername,
        String ownerDisplayName,
        int members,
        String coverage,
        String description
    ) {
        if (dutyScheduleRepository.countGroupByName(name) == 0) {
            DutyGroup dutyGroup = DutyGroup.builder()
                .name(name)
                .ownerUsername(ownerUsername)
                .ownerDisplayName(ownerDisplayName)
                .members(members)
                .coverage(coverage)
                .description(description)
                .build();
            dutyScheduleRepository.insertGroup(dutyGroup);
            return dutyGroup;
        }

        return dutyScheduleRepository.findAllGroups().stream()
            .filter(group -> name.equals(group.getName()))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("值班组不存在: " + name));
    }

    private void seedShift(
        DutyGroup group,
        LocalDate dutyDate,
        String dateLabel,
        String shiftTime,
        String ownerDisplayName,
        String ownerUsername,
        String status
    ) {
        if (dutyScheduleRepository.countShiftByDateAndGroup(group.getId(), dutyDate.toString()) > 0) {
            return;
        }

        dutyScheduleRepository.insertShift(DutyShift.builder()
            .groupId(group.getId())
            .groupName(group.getName())
            .dutyDate(dutyDate)
            .dateLabel(dateLabel)
            .shiftLabel(dutyDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + " " + weekday(dutyDate))
            .shiftTime(shiftTime)
            .ownerDisplayName(ownerDisplayName)
            .ownerUsername(ownerUsername)
            .status(status)
            .build());
    }

    private void seedWorkOrders(AppUser demoUser) {
        if (workOrderRepository.count() > 0) {
            return;
        }
        ObjectNode template = platformTemplateService.copyTemplate();
        ObjectNode page = (ObjectNode) template.with("pages").get("/personSetting/orderManage");
        ArrayNode rows = page.withArray("rows");
        for (JsonNode row : rows) {
            WorkOrder workOrder = WorkOrder.builder()
                .orderNo(row.path("id").asText())
                .title(row.path("title").asText())
                .type(row.path("type").asText())
                .creatorUsername(demoUser.getUsername())
                .creatorDisplayName(row.path("creator").asText("演示用户"))
                .progress(defaultIfBlank(row.path("progress").asText(), "主管受理"))
                .status(defaultIfBlank(row.path("status").asText(), "待受理"))
                .priority("中")
                .serviceName(row.path("title").asText())
                .description(row.path("title").asText())
                .estimatedAt(row.path("eta").asText("--"))
                .createdAt(DateFormats.parseFlexible(row.path("createdAt").asText()))
                .updatedAt(DateFormats.parseFlexible(row.path("createdAt").asText()))
                .processCode(WorkOrderProcessService.DEFAULT_PROCESS_CODE)
                .currentNodeCode(resolveSeedNode(row.path("status").asText()))
                .currentNodeName(resolveSeedNodeName(row.path("status").asText()))
                .currentHandler("管理员")
                .build();
            workOrderRepository.insert(workOrder);
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
            taskRecordRepository.insert(TaskRecord.builder()
                .taskNo(row.path("id").asText())
                .title(row.path("title").asText())
                .source(row.path("source").asText())
                .ticket(row.path("ticket").asText())
                .status(row.path("status").asText())
                .assignee(row.path("assignee").asText())
                .priority(row.path("priority").asText("中"))
                .creator(row.path("creator").asText())
                .createdAt(DateFormats.parseFlexible(row.path("createdAt").asText()))
                .orderNo(row.path("ticket").asText())
                .nodeCode("ENGINEER_HANDLE")
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
            notificationMessageRepository.insert(NotificationMessage.builder()
                .title(row.path("title").asText())
                .messageType(row.path("type").asText("工作台"))
                .sentAt(DateFormats.parseFlexible(row.path("time").asText()))
                .read(false)
                .recipientUsername("demo")
                .sourceType("BOOTSTRAP")
                .sourceId(row.path("title").asText())
                .build());
        }
    }

    private String weekday(LocalDate dutyDate) {
        switch (dutyDate.getDayOfWeek()) {
            case MONDAY:
                return "周一";
            case TUESDAY:
                return "周二";
            case WEDNESDAY:
                return "周三";
            case THURSDAY:
                return "周四";
            case FRIDAY:
                return "周五";
            case SATURDAY:
                return "周六";
            default:
                return "周日";
        }
    }

    private String resolveSeedNode(String status) {
        if (status.contains("结束") || status.contains("完成")) {
            return "END_DONE";
        }
        if (status.contains("驳回")) {
            return "END_REJECTED";
        }
        if (status.contains("确认")) {
            return "REQUESTER_CONFIRM";
        }
        if (status.contains("进行")) {
            return "ENGINEER_HANDLE";
        }
        return "TRIAGE";
    }

    private String resolveSeedNodeName(String status) {
        if ("END_DONE".equals(resolveSeedNode(status))) {
            return "已完成";
        }
        if ("END_REJECTED".equals(resolveSeedNode(status))) {
            return "已驳回";
        }
        if ("REQUESTER_CONFIRM".equals(resolveSeedNode(status))) {
            return "申请人确认";
        }
        if ("ENGINEER_HANDLE".equals(resolveSeedNode(status))) {
            return "工程师处理";
        }
        return "主管受理";
    }

    private String defaultIfBlank(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }

    private void seedNetworkDevices() {
        if (networkDeviceRepository.count() > 0) {
            return;
        }
        Object[][] devices = {
            {"DEV-CORE-R01", "Cisco-核心路由器", "192.168.1.1", "路由器", "Cisco", "SNMP", "public", "v2c", null, 161, "待采集", "核心出口路由器"},
            {"DEV-CORE-SW01", "华为-核心交换机", "192.168.1.2", "交换机", "华为", "SNMP", "public", "v2c", null, 161, "待采集", "核心层三层交换机"},
            {"DEV-AGG-SW01", "H3C-汇聚交换机", "192.168.1.3", "交换机", "H3C", "SNMP", "public", "v2c", null, 161, "待采集", "汇聚层交换机"},
            {"DEV-FW-01", "华为-边界防火墙", "10.0.0.1", "防火墙", "华为", "SSH", null, null, "netadmin", 22, "待采集", "互联网边界防火墙"},
            {"DEV-FW-02", "思科-内部防火墙", "10.0.0.2", "防火墙", "Cisco", "SNMP", "private", "v2c", null, 161, "待采集", "内部区域防火墙"},
        };
        for (Object[] row : devices) {
            NetworkDevice device = NetworkDevice.builder()
                .deviceCode((String) row[0])
                .name((String) row[1])
                .ip((String) row[2])
                .deviceType((String) row[3])
                .vendor((String) row[4])
                .protocol((String) row[5])
                .snmpCommunity((String) row[6])
                .snmpVersion((String) row[7])
                .sshUsername((String) row[8])
                .port((Integer) row[9])
                .status((String) row[10])
                .description((String) row[11])
                .build();
            networkDeviceRepository.insert(device);
        }
    }

    private void seedCollectionTemplates() {
        if (collectionTemplateRepository.count() > 0) {
            return;
        }
        String snmpMetrics = "[{\"oid\":\"1.3.6.1.2.1.2.2.1.10\",\"name\":\"接口流量(Mbps)\",\"type\":\"interface_traffic\"},"
            + "{\"oid\":\"1.3.6.1.2.1.2.2.1.14\",\"name\":\"错包率(%)\",\"type\":\"error_rate\"},"
            + "{\"oid\":\"1.3.6.1.4.1.9.9.109.1.1.1.1.6\",\"name\":\"CPU利用率(%)\",\"type\":\"cpu_usage\"},"
            + "{\"oid\":\"1.3.6.1.4.1.9.9.48.1.1.1.5\",\"name\":\"内存利用率(%)\",\"type\":\"mem_usage\"}]";
        String routerMetrics = "[{\"oid\":\"1.3.6.1.2.1.2.2.1.10\",\"name\":\"接口流量(Mbps)\",\"type\":\"interface_traffic\"},"
            + "{\"oid\":\"1.3.6.1.2.1.15.3.1.2\",\"name\":\"BGP邻居状态\",\"type\":\"bgp_status\"},"
            + "{\"oid\":\"1.3.6.1.2.1.14.10.1.6\",\"name\":\"OSPF邻居状态\",\"type\":\"ospf_status\"},"
            + "{\"oid\":\"1.3.6.1.4.1.9.9.109.1.1.1.1.6\",\"name\":\"CPU利用率(%)\",\"type\":\"cpu_usage\"},"
            + "{\"oid\":\"1.3.6.1.4.1.9.9.48.1.1.1.5\",\"name\":\"内存利用率(%)\",\"type\":\"mem_usage\"}]";

        CollectionTemplate t1 = CollectionTemplate.builder()
            .templateCode("TPL-SWITCH-SNMP")
            .name("通用交换机 SNMP 模板")
            .deviceType("交换机")
            .protocol("SNMP")
            .metricsJson(snmpMetrics)
            .intervalSeconds(300)
            .enabled(true)
            .description("适用于 Cisco/华为/H3C 交换机，采集接口流量、错包率、CPU/内存利用率")
            .build();
        collectionTemplateRepository.insert(t1);

        CollectionTemplate t2 = CollectionTemplate.builder()
            .templateCode("TPL-ROUTER-SNMP")
            .name("核心路由器 SNMP 模板")
            .deviceType("路由器")
            .protocol("SNMP")
            .metricsJson(routerMetrics)
            .intervalSeconds(60)
            .enabled(true)
            .description("路由器专用模板，额外采集 BGP/OSPF 邻居状态，采集周期更短")
            .build();
        collectionTemplateRepository.insert(t2);

        CollectionTemplate t3 = CollectionTemplate.builder()
            .templateCode("TPL-FW-SSH")
            .name("防火墙 SSH 采集模板")
            .deviceType("防火墙")
            .protocol("SSH")
            .metricsJson("[{\"cmd\":\"show cpu\",\"name\":\"CPU利用率(%)\",\"type\":\"cpu_usage\"},"
                + "{\"cmd\":\"show memory\",\"name\":\"内存利用率(%)\",\"type\":\"mem_usage\"},"
                + "{\"cmd\":\"show interface\",\"name\":\"接口流量(Mbps)\",\"type\":\"interface_traffic\"}]")
            .intervalSeconds(120)
            .enabled(true)
            .description("通过 SSH 命令行采集防火墙性能指标，适用于不支持 SNMP 的设备")
            .build();
        collectionTemplateRepository.insert(t3);
    }

    private void seedAlertRules() {
        if (alertRuleRepository.count() > 0) {
            return;
        }
        Object[][] rules = {
            {"RULE-CPU-HIGH",    "CPU利用率过高",        "cpu_usage",         "> 90%",  "90",  "严重",     true,  "基础设施组", "主机CPU连续5分钟超90%触发告警"},
            {"RULE-MEM-HIGH",    "内存利用率过高",        "mem_usage",         "> 85%",  "85",  "严重",     true,  "基础设施组", "内存使用率超85%触发"},
            {"RULE-DISK-HIGH",   "磁盘使用率过高",        "disk_usage",        "> 80%",  "80",  "警告",     true,  "基础设施组", "磁盘使用率超80%触发"},
            {"RULE-NET-ERR",     "网络接口错包率异常",     "error_rate",        "> 1%",   "1",   "警告",     true,  "网络组",    "接口错包率超1%触发"},
            {"RULE-BGP-DOWN",    "BGP邻居断连",          "bgp_status",        "!= Established", "", "灾难",  true,  "网络组",    "BGP邻居状态不为Established"},
            {"RULE-MYSQL-SLOW",  "MySQL慢查询过多",       "mysql_slow_query",  "> 100/min", "100", "警告",  true,  "数据库组",  "慢查询每分钟超100次"},
            {"RULE-REDIS-MEM",   "Redis内存使用率高",     "redis_mem_usage",   "> 80%",  "80",  "警告",     true,  "数据库组",  "Redis内存使用率超80%"},
            {"RULE-KAFKA-LAG",   "Kafka消费Lag过大",     "kafka_consumer_lag","> 10000","10000","严重",    true,  "中间件组",  "消费者Lag超1万条"},
            {"RULE-POD-RESTART", "Pod频繁重启",           "pod_restart_count", "> 5次",  "5",   "严重",     true,  "容器组",    "Pod 10分钟内重启超5次"},
            {"RULE-HTTP-5XX",    "HTTP 5xx错误率高",      "http_5xx_rate",     "> 5%",   "5",   "严重",     true,  "应用组",    "接口5xx错误率超5%"},
            {"RULE-JVM-GC",      "JVM Full GC频繁",       "jvm_fullgc_count",  "> 3次",  "3",   "警告",     true,  "应用组",    "Full GC每小时超3次"},
            {"RULE-PROBE-FAIL",  "服务探测失败",           "probe_status",      "失败",   "",    "严重",     true,  "运维组",    "服务探测连续3次失败"},
        };
        for (Object[] row : rules) {
            AlertRule rule = AlertRule.builder()
                .ruleCode((String) row[0])
                .name((String) row[1])
                .metricType((String) row[2])
                .conditionExpr((String) row[3])
                .threshold((String) row[4])
                .severity((String) row[5])
                .enabled((Boolean) row[6])
                .notifyGroup((String) row[7])
                .description((String) row[8])
                .build();
            alertRuleRepository.insert(rule);
        }
    }

    private void seedServiceProbes() {
        if (serviceProbeRepository.count() > 0) {
            return;
        }
        Object[][] probes = {
            {"PROBE-API-GW",   "API网关可用性探测",  "HTTP",  "http://api.opsany.internal/health",      "HTTP",  30, 3000, "运行中", "200ms", "探测API网关健康检查接口"},
            {"PROBE-WEB-MAIN", "主站HTTP探测",       "HTTP",  "http://www.opsany.internal",              "HTTP",  60, 5000, "运行中", "186ms", "监控主站HTTP响应时间"},
            {"PROBE-DNS-RES",  "DNS解析探测",        "DNS",   "dns.opsany.internal",                     "DNS",   30, 1000, "运行中", "正常",  "探测内网DNS解析可用性"},
            {"PROBE-DB-PORT",  "MySQL端口探测",      "TCP",   "db-master.opsany.internal:3306",          "TCP",   60, 2000, "运行中", "正常",  "探测MySQL主库端口可达性"},
            {"PROBE-REDIS-PORT","Redis端口探测",     "TCP",   "redis-primary.opsany.internal:6379",      "TCP",   60, 1000, "运行中", "正常",  "探测Redis主节点端口可达性"},
            {"PROBE-K8S-API",  "K8s APIServer探测",  "HTTPS", "https://k8s-api.opsany.internal:6443",   "HTTPS", 30, 3000, "运行中", "142ms", "探测K8s APIServer可用性"},
            {"PROBE-KAFKA-TCP","Kafka端口探测",      "TCP",   "kafka-broker01.opsany.internal:9092",     "TCP",   60, 2000, "运行中", "正常",  "探测Kafka Broker端口"},
            {"PROBE-AUTH-SVC", "认证服务探测",        "HTTP",  "http://auth.opsany.internal/api/health", "HTTP",  30, 3000, "异常",   "超时",  "探测SSO认证服务"},
        };
        for (Object[] row : probes) {
            ServiceProbe probe = ServiceProbe.builder()
                .probeCode((String) row[0])
                .name((String) row[1])
                .probeType((String) row[2])
                .targetUrl((String) row[3])
                .protocol((String) row[4])
                .intervalSeconds((Integer) row[5])
                .timeoutMs((Integer) row[6])
                .status((String) row[7])
                .lastResult((String) row[8])
                .description((String) row[9])
                .build();
            serviceProbeRepository.insert(probe);
        }
    }

    private void seedLogCollectors() {
        if (logCollectorRepository.count() > 0) {
            return;
        }
        Object[][] collectors = {
            {"LC-NGINX-ACCESS",  "Nginx访问日志采集",    "File",    "/var/log/nginx/access.log",          "logstash-01.opsany.internal", "UTF-8", "运行中", 2847392L, "采集Nginx访问日志到ES"},
            {"LC-NGINX-ERROR",   "Nginx错误日志采集",    "File",    "/var/log/nginx/error.log",           "logstash-01.opsany.internal", "UTF-8", "运行中", 12843L,   "采集Nginx错误日志"},
            {"LC-APP-STDOUT",    "应用标准输出日志",      "Docker",  "container://app-*",                  "logstash-02.opsany.internal", "UTF-8", "运行中", 9384721L, "采集K8s容器标准输出日志"},
            {"LC-MYSQL-SLOW",    "MySQL慢查询日志",       "File",    "/var/log/mysql/mysql-slow.log",      "logstash-01.opsany.internal", "UTF-8", "运行中", 48293L,   "采集MySQL慢查询日志"},
            {"LC-SYSLOG",        "系统Syslog采集",        "Syslog",  "udp://0.0.0.0:514",                  "logstash-02.opsany.internal", "UTF-8", "运行中", 3847291L, "采集所有主机系统日志"},
            {"LC-FW-SYSLOG",     "防火墙安全日志",        "Syslog",  "udp://0.0.0.0:5140",                 "logstash-03.opsany.internal", "UTF-8", "运行中", 892341L,  "采集防火墙Syslog安全事件"},
            {"LC-K8S-AUDIT",     "K8s审计日志采集",       "File",    "/var/log/kubernetes/audit.log",      "logstash-02.opsany.internal", "UTF-8", "运行中", 284932L,  "采集K8s APIServer审计日志"},
            {"LC-JAVA-APP",      "Java应用日志采集",      "File",    "/app/logs/*.log",                    "logstash-01.opsany.internal", "UTF-8", "运行中", 7483921L, "采集Java应用日志文件"},
            {"LC-REDIS-LOG",     "Redis日志采集",         "File",    "/var/log/redis/redis-server.log",    "logstash-01.opsany.internal", "UTF-8", "运行中", 48231L,   "采集Redis服务日志"},
            {"LC-KAFKA-LOG",     "Kafka Broker日志",      "File",    "/opt/kafka/logs/server.log",         "logstash-03.opsany.internal", "UTF-8", "异常",   0L,       "采集Kafka Broker运行日志"},
        };
        for (Object[] row : collectors) {
            LogCollector collector = LogCollector.builder()
                .collectorCode((String) row[0])
                .name((String) row[1])
                .sourceType((String) row[2])
                .sourcePath((String) row[3])
                .targetHost((String) row[4])
                .encoding((String) row[5])
                .status((String) row[6])
                .linesCollected((Long) row[7])
                .description((String) row[8])
                .build();
            logCollectorRepository.insert(collector);
        }
    }

    private void seedMetricSnapshots() {
        if (metricSnapshotRepository.count() > 0) {
            return;
        }
        Object[][] metrics = {
            {"DEV-CORE-R01", "interface_traffic", "GE0/0接口流量(Mbps)", "1253.6"},
            {"DEV-CORE-R01", "cpu_usage",         "CPU利用率(%)",         "34"},
            {"DEV-CORE-R01", "mem_usage",         "内存利用率(%)",         "58"},
            {"DEV-CORE-R01", "bgp_status",        "BGP邻居状态",           "Established(2/2)"},
            {"DEV-CORE-R01", "ospf_status",       "OSPF邻居状态",          "Full(3/3)"},
            {"DEV-CORE-SW01","interface_traffic", "GE1/0/1接口流量(Mbps)", "421.8"},
            {"DEV-CORE-SW01","error_rate",        "错包率(%)",             "0.02"},
            {"DEV-CORE-SW01","cpu_usage",         "CPU利用率(%)",          "12"},
            {"DEV-CORE-SW01","mem_usage",         "内存利用率(%)",          "37"},
            {"DEV-AGG-SW01", "interface_traffic", "GE2/0/1接口流量(Mbps)", "186.4"},
            {"DEV-AGG-SW01", "cpu_usage",         "CPU利用率(%)",          "8"},
            {"DEV-FW-01",    "cpu_usage",         "CPU利用率(%)",          "21"},
            {"DEV-FW-01",    "mem_usage",         "内存利用率(%)",          "44"},
            {"DEV-FW-01",    "interface_traffic", "eth0接口流量(Mbps)",    "95.3"},
            {"DEV-FW-02",    "cpu_usage",         "CPU利用率(%)",          "19"},
            {"DEV-FW-02",    "interface_traffic", "GE0接口流量(Mbps)",     "62.1"},
        };
        for (Object[] row : metrics) {
            MetricSnapshot snapshot = MetricSnapshot.builder()
                .deviceId(0L)
                .deviceCode((String) row[0])
                .metricType((String) row[1])
                .metricName((String) row[2])
                .metricValue((String) row[3])
                .build();
            metricSnapshotRepository.insert(snapshot);
        }
    }

    private void seedInfraServers() {
        if (infraServerRepository.count() > 0) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        Object[][] servers = {
            {"SRV-WEB-01", "web-server-01", "10.10.1.11", "Dell R740", "34%", "61%", "55%", "42°C", "Agent", "正常"},
            {"SRV-WEB-02", "web-server-02", "10.10.1.12", "Dell R740", "28%", "58%", "48%", "38°C", "Agent", "正常"},
            {"SRV-APP-01", "app-server-01", "10.10.2.11", "HP DL380", "67%", "75%", "70%", "51°C", "Agent", "正常"},
            {"SRV-APP-02", "app-server-02", "10.10.2.12", "HP DL380", "91%", "88%", "65%", "68°C", "Agent", "告警"},
            {"SRV-DB-01",  "db-server-01",  "10.10.3.11", "Dell R940", "45%", "82%", "78%", "47°C", "SNMP",  "正常"},
            {"SRV-DB-02",  "db-server-02",  "10.10.3.12", "Dell R940", "38%", "79%", "74%", "44°C", "SNMP",  "正常"},
            {"SRV-K8S-01", "k8s-node-01",   "10.10.4.11", "华为 2288H", "72%", "69%", "60%", "55°C", "Agent", "正常"},
            {"SRV-K8S-02", "k8s-node-02",   "10.10.4.12", "华为 2288H", "68%", "71%", "58%", "52°C", "Agent", "正常"},
            {"SRV-K8S-03", "k8s-node-03",   "10.10.4.13", "华为 2288H", "55%", "64%", "62%", "48°C", "Agent", "正常"},
            {"SRV-MON-01", "monitor-server","10.10.5.11", "Dell R740", "22%", "54%", "45%", "36°C", "Agent", "正常"},
        };
        for (Object[] row : servers) {
            infraServerRepository.insert(InfraServer.builder()
                .serverCode((String) row[0])
                .hostname((String) row[1])
                .ip((String) row[2])
                .model((String) row[3])
                .cpuUsage((String) row[4])
                .memUsage((String) row[5])
                .diskUsage((String) row[6])
                .cpuTemp((String) row[7])
                .collectMethod((String) row[8])
                .status((String) row[9])
                .lastCollectedAt(now)
                .createdAt(now)
                .updatedAt(now)
                .build());
        }
    }

    private void seedK8sClusters() {
        if (k8sClusterRepository.count() > 0) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        Object[][] clusters = {
            {"K8S-PROD-01",  "生产集群",     "1.28.3",  12, 186, "52%", "68%", "正常"},
            {"K8S-STAGING",  "预发布集群",   "1.28.1",   6,  74, "38%", "55%", "正常"},
            {"K8S-DEV",      "开发集群",     "1.27.8",   3,  28, "21%", "41%", "正常"},
            {"K8S-MONITOR",  "监控集群",     "1.28.0",   4,  35, "45%", "61%", "告警"},
        };
        for (Object[] row : clusters) {
            k8sClusterRepository.insert(K8sCluster.builder()
                .clusterCode((String) row[0])
                .clusterName((String) row[1])
                .version((String) row[2])
                .nodeCount((Integer) row[3])
                .podCount((Integer) row[4])
                .cpuUsage((String) row[5])
                .memUsage((String) row[6])
                .status((String) row[7])
                .lastCollectedAt(now)
                .createdAt(now)
                .updatedAt(now)
                .build());
        }
    }

    private void seedK8sPods() {
        if (k8sPodRepository.count() > 0) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        Object[][] pods = {
            {"POD-001", "K8S-PROD-01", "production", "api-gateway-6d9f8b-xkp2q",  "k8s-node-01", "12%", "256Mi", 0, "运行中"},
            {"POD-002", "K8S-PROD-01", "production", "user-service-7b8c4d-mnpqr", "k8s-node-02", "8%",  "128Mi", 0, "运行中"},
            {"POD-003", "K8S-PROD-01", "production", "order-service-5f6e-lkjhg",  "k8s-node-03", "15%", "512Mi", 2, "重启中"},
            {"POD-004", "K8S-PROD-01", "production", "payment-svc-2a3b4c-zxcvb",  "k8s-node-01", "22%", "1Gi",   8, "告警"},
            {"POD-005", "K8S-PROD-01", "monitoring", "prometheus-0",               "k8s-node-02", "18%", "2Gi",   0, "运行中"},
            {"POD-006", "K8S-STAGING","staging",     "api-gateway-staging-abc12",  "k8s-node-01", "6%",  "128Mi", 0, "运行中"},
        };
        for (Object[] row : pods) {
            k8sPodRepository.insert(K8sPod.builder()
                .podCode((String) row[0])
                .clusterCode((String) row[1])
                .namespace((String) row[2])
                .podName((String) row[3])
                .nodeName((String) row[4])
                .cpuUsage((String) row[5])
                .memUsage((String) row[6])
                .restartCount((Integer) row[7])
                .status((String) row[8])
                .createdAt(now)
                .build());
        }
    }

    private void seedMiddlewareInstances() {
        if (middlewareInstanceRepository.count() > 0) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        Object[][] instances = {
            {"MW-MYSQL-MASTER", "MySQL主库",     "MySQL",      "8.0.32", "10.10.3.11", 3306, "38%", "62%", "142",  "2380/s", "正常"},
            {"MW-MYSQL-SLAVE",  "MySQL从库",     "MySQL",      "8.0.32", "10.10.3.12", 3306, "22%", "55%", "87",   "1240/s", "正常"},
            {"MW-REDIS-01",     "Redis主节点",   "Redis",      "7.0.5",  "10.10.3.21", 6379, "12%", "78%", "1248", "18500/s","正常"},
            {"MW-REDIS-02",     "Redis副本",     "Redis",      "7.0.5",  "10.10.3.22", 6379, "8%",  "72%", "843",  "12300/s","正常"},
            {"MW-KAFKA-01",     "Kafka Broker1", "Kafka",      "3.5.0",  "10.10.4.21", 9092, "45%", "68%", "284",  "48000/s","正常"},
            {"MW-KAFKA-02",     "Kafka Broker2", "Kafka",      "3.5.0",  "10.10.4.22", 9092, "51%", "71%", "261",  "46000/s","告警"},
            {"MW-ES-01",        "Elasticsearch", "Elasticsearch","8.9.0","10.10.5.21", 9200, "62%", "85%", "54",   "3200/s", "正常"},
            {"MW-NGINX-01",     "Nginx网关",     "Nginx",      "1.24.0", "10.10.1.1",  80,   "14%", "22%", "8642", "12400/s","正常"},
        };
        for (Object[] row : instances) {
            middlewareInstanceRepository.insert(MiddlewareInstance.builder()
                .instanceCode((String) row[0])
                .name((String) row[1])
                .middlewareType((String) row[2])
                .version((String) row[3])
                .host((String) row[4])
                .port((Integer) row[5])
                .cpuUsage((String) row[6])
                .memUsage((String) row[7])
                .connectCount((String) row[8])
                .qps((String) row[9])
                .status((String) row[10])
                .lastCollectedAt(now)
                .createdAt(now)
                .updatedAt(now)
                .build());
        }
    }

    private void seedStorageNodes() {
        if (storageNodeRepository.count() > 0) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        Object[][] nodes = {
            {"NODE-ES-01",  "es-data-01:9200",  "数据节点", "128GB", "500GB", "125MB/s", "340/s",  "正常"},
            {"NODE-ES-02",  "es-data-02:9200",  "数据节点", "116GB", "500GB", "112MB/s", "320/s",  "正常"},
            {"NODE-ES-03",  "es-data-03:9200",  "数据节点", "94GB",  "500GB", "98MB/s",  "280/s",  "正常"},
            {"NODE-ES-MSTR","es-master:9200",   "主节点",   "12GB",  "50GB",  "8MB/s",   "120/s",  "正常"},
            {"NODE-PROM-01","prometheus:9090",  "存储节点", "68GB",  "200GB", "24MB/s",  "8400/s", "正常"},
            {"NODE-VT-01",  "victoriametrics:8428","时序存储","152GB","400GB","88MB/s",  "95000/s","正常"},
        };
        for (Object[] row : nodes) {
            storageNodeRepository.insert(StorageNode.builder()
                .nodeCode((String) row[0])
                .node((String) row[1])
                .role((String) row[2])
                .used((String) row[3])
                .total((String) row[4])
                .writeRate((String) row[5])
                .queryRate((String) row[6])
                .status((String) row[7])
                .updatedAt(now)
                .build());
        }
    }

    private void seedArchivePolicies() {
        if (archivePolicyRepository.count() > 0) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        Object[][] policies = {
            {"POL-METRIC-01",   "指标数据归档策略",     "metrics",       7,  30,  180, 1, "启用"},
            {"POL-LOG-01",      "日志数据归档策略",     "logs",          3,  14,   90, 1, "启用"},
            {"POL-ALERT-01",    "告警历史归档策略",     "alert_events",  30, 90,  365, 0, "启用"},
            {"POL-TRACE-01",    "链路追踪归档策略",     "traces",        2,   7,   30, 1, "启用"},
            {"POL-AUDIT-01",    "审计日志长期归档",     "audit_logs",    7,  30, 1095, 1, "启用"},
        };
        for (Object[] row : policies) {
            archivePolicyRepository.insert(ArchivePolicy.builder()
                .policyCode((String) row[0])
                .policyName((String) row[1])
                .targetType((String) row[2])
                .hotDays((Integer) row[3])
                .warmDays((Integer) row[4])
                .coldDays((Integer) row[5])
                .compress((Integer) row[6])
                .status((String) row[7])
                .createdAt(now)
                .updatedAt(now)
                .build());
        }
    }

    private void seedArchiveHistory() {
        if (archiveHistoryRepository.count() > 0) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        Object[][] history = {
            {"POL-METRIC-01", "指标数据归档策略", "12.4GB", 125, "成功"},
            {"POL-LOG-01",    "日志数据归档策略", "38.7GB",  84, "成功"},
            {"POL-ALERT-01",  "告警历史归档策略",  "2.1GB",  18, "成功"},
            {"POL-METRIC-01", "指标数据归档策略", "13.2GB", 131, "成功"},
            {"POL-TRACE-01",  "链路追踪归档策略",  "8.9GB",  62, "失败"},
            {"POL-LOG-01",    "日志数据归档策略", "41.3GB",  97, "成功"},
        };
        for (Object[] row : history) {
            archiveHistoryRepository.insert(ArchiveHistory.builder()
                .policyCode((String) row[0])
                .policy((String) row[1])
                .dataSize((String) row[2])
                .duration((Integer) row[3])
                .status((String) row[4])
                .executedAt(now.minusDays((long)(Math.random() * 14)))
                .build());
        }
    }

    private void seedNotifyChannels() {
        if (notifyChannelRepository.count() > 0) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        Object[][] channels = {
            {"CHAN-SITE-01",  "站内消息",    "站内",    "内置站内消息系统",            284, 0, "正常"},
            {"CHAN-EMAIL-01", "邮件通知",    "邮件",    "smtp.opsany.internal:465",    162, 3, "正常"},
            {"CHAN-WECHAT-01","企业微信",    "企业微信","企业微信WebHook",             198, 1, "正常"},
            {"CHAN-DING-01",  "钉钉群机器人","钉钉",    "钉钉群组Webhook",              89, 0, "正常"},
            {"CHAN-SMS-01",   "短信通知",    "短信",    "阿里云短信服务",               45, 2, "异常"},
            {"CHAN-PD-01",    "PagerDuty",   "Webhook", "https://events.pagerduty.com", 12, 0, "正常"},
        };
        for (Object[] row : channels) {
            notifyChannelRepository.insert(NotifyChannel.builder()
                .channelCode((String) row[0])
                .channelName((String) row[1])
                .channelType((String) row[2])
                .config((String) row[3])
                .sentToday((Integer) row[4])
                .failToday((Integer) row[5])
                .status((String) row[6])
                .createdAt(now)
                .updatedAt(now)
                .build());
        }
    }

    private void seedAlertEvents() {
        if (alertEventRepository.count() > 0) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        Object[][] events = {
            {"EVT-001", "CPU利用率过高",    "app-server-02",       "严重", "已恢复", "张工",   now.minusHours(2),  now.minusHours(1)},
            {"EVT-002", "Kafka消费Lag过大", "kafka-broker02",      "严重", "处理中", "李工",   now.minusHours(1),  null},
            {"EVT-003", "磁盘使用率过高",    "db-server-01",        "警告", "已恢复", "王工",   now.minusHours(3),  now.minusHours(2)},
            {"EVT-004", "服务探测失败",      "auth.opsany.internal","严重", "处理中", "运维组", now.minusMinutes(30), null},
            {"EVT-005", "Pod频繁重启",       "payment-svc Pod",     "严重", "已恢复", "张工",   now.minusHours(5),  now.minusHours(4)},
            {"EVT-006", "Redis内存使用率高", "redis-primary",       "警告", "已忽略", "李工",   now.minusDays(1),   now.minusDays(1)},
            {"EVT-007", "BGP邻居断连",       "core-router-01",      "灾难", "已恢复", "网络组", now.minusDays(2),   now.minusDays(2)},
            {"EVT-008", "JVM Full GC频繁",   "order-service",       "警告", "处理中", "王工",   now.minusMinutes(15), null},
            {"EVT-009", "HTTP 5xx错误率高",  "api-gateway",         "严重", "已恢复", "张工",   now.minusHours(6),  now.minusHours(5)},
            {"EVT-010", "内存利用率过高",     "app-server-02",       "严重", "处理中", "李工",   now.minusMinutes(45), null},
        };
        for (Object[] row : events) {
            alertEventRepository.insert(AlertEvent.builder()
                .eventCode((String) row[0])
                .ruleName((String) row[1])
                .source((String) row[2])
                .severity((String) row[3])
                .status((String) row[4])
                .handler((String) row[5])
                .triggeredAt((LocalDateTime) row[6])
                .resolvedAt((LocalDateTime) row[7])
                .build());
        }
    }

    private void seedOnCallSchedules() {
        if (onCallScheduleRepository.count() > 0) {
            return;
        }
        LocalDate today = LocalDate.now();
        String[] weekdays = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};
        Object[][] schedules = {
            {today.minusDays(2), "基础设施值班组", "管理员", "夜班 00:00-08:00",  "已完成"},
            {today.minusDays(1), "应用运维组",     "演示用户","日班 09:00-18:00", "已完成"},
            {today,              "数据库组",       "管理员", "全天 00:00-24:00",  "当班中"},
            {today.plusDays(1),  "基础设施值班组", "演示用户","夜班 00:00-08:00", "待值班"},
            {today.plusDays(2),  "应用运维组",     "管理员", "日班 09:00-18:00",  "待值班"},
            {today.plusDays(3),  "数据库组",       "演示用户","全天 00:00-24:00", "待值班"},
            {today.plusDays(4),  "基础设施值班组", "管理员", "夜班 00:00-08:00",  "待值班"},
        };
        for (Object[] row : schedules) {
            LocalDate dutyDate = (LocalDate) row[0];
            String weekday = weekdays[dutyDate.getDayOfWeek().getValue() - 1];
            onCallScheduleRepository.insert(OnCallSchedule.builder()
                .dutyDate(dutyDate)
                .weekday(weekday)
                .groupName((String) row[1])
                .person((String) row[2])
                .shift((String) row[3])
                .status((String) row[4])
                .build());
        }
    }

    private void seedResponseRecords() {
        if (responseRecordRepository.count() > 0) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        Object[][] records = {
            {"CPU利用率过高",    "张工",   3,  45, "已解决"},
            {"Kafka消费Lag过大", "李工",   8,  null, "处理中"},
            {"BGP邻居断连",      "网络组", 2,  18, "已解决"},
            {"服务探测失败",      "运维组", 5,  null, "处理中"},
            {"Pod频繁重启",      "张工",   4,  32, "已解决"},
            {"磁盘使用率过高",    "王工",   6,  55, "已解决"},
            {"HTTP 5xx错误率高", "张工",   2,  28, "已解决"},
        };
        for (Object[] row : records) {
            responseRecordRepository.insert(ResponseRecord.builder()
                .alertName((String) row[0])
                .responder((String) row[1])
                .responseMin((Integer) row[2])
                .resolveMin((Integer) row[3])
                .result((String) row[4])
                .triggeredAt(now.minusHours((long)(Math.random() * 48 + 1)))
                .build());
        }
    }

    private void seedDashboards() {
        if (dashboardRepository.count() > 0) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        Object[][] dashboards = {
            {"DASH-INFRA-01",   "基础设施总览",   "基础设施", 12, "admin", 1, 48},
            {"DASH-K8S-01",     "Kubernetes监控", "容器云",   18, "admin", 1, 36},
            {"DASH-APP-01",     "应用性能监控",   "应用",     15, "demo",  1, 42},
            {"DASH-ALERT-01",   "告警概览",       "告警",      8, "admin", 1, 65},
            {"DASH-DB-01",      "数据库监控",     "中间件",   10, "demo",  0, 28},
            {"DASH-NET-01",     "网络拓扑监控",   "网络",      6, "admin", 0, 19},
            {"DASH-BUSINESS-01","业务大盘",       "业务",     14, "demo",  1, 87},
        };
        for (Object[] row : dashboards) {
            dashboardRepository.insert(Dashboard.builder()
                .dashboardCode((String) row[0])
                .name((String) row[1])
                .category((String) row[2])
                .charts((Integer) row[3])
                .creator((String) row[4])
                .shared((Integer) row[5])
                .visitToday((Integer) row[6])
                .updatedAt(now)
                .createdAt(now)
                .build());
        }
    }

    private void seedTopologyItems() {
        if (topologyItemRepository.count() > 0) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        Object[][] topologies = {
            {"TOPO-FULL",   "全局拓扑图",       "全局",     42, 68, 2, 1},
            {"TOPO-NET",    "网络设备拓扑",     "网络",     12, 18, 0, 1},
            {"TOPO-K8S",    "K8s集群拓扑",      "容器",     35, 52, 1, 0},
            {"TOPO-APP",    "应用服务依赖图",   "应用",     28, 45, 3, 0},
            {"TOPO-DB",     "数据库集群拓扑",   "数据库",    8, 12, 0, 1},
        };
        for (Object[] row : topologies) {
            topologyItemRepository.insert(TopologyItem.builder()
                .topologyCode((String) row[0])
                .name((String) row[1])
                .type((String) row[2])
                .nodeCount((Integer) row[3])
                .linkCount((Integer) row[4])
                .abnormal((Integer) row[5])
                .autoRefresh((Integer) row[6])
                .updatedAt(now)
                .createdAt(now)
                .build());
        }
    }

    private void seedTopologyLinks() {
        if (topologyLinkRepository.count() > 0) {
            return;
        }
        Object[][] links = {
            {"TOPO-APP", "api-gateway",     "user-service",   "HTTP",  "12ms",  "0.1%", "正常"},
            {"TOPO-APP", "api-gateway",     "order-service",  "HTTP",  "18ms",  "2.3%", "告警"},
            {"TOPO-APP", "api-gateway",     "payment-svc",    "HTTP",  "25ms",  "4.8%", "告警"},
            {"TOPO-APP", "order-service",   "MySQL主库",      "JDBC",  "8ms",   "0.0%", "正常"},
            {"TOPO-APP", "order-service",   "Redis主节点",    "Redis", "2ms",   "0.0%", "正常"},
            {"TOPO-APP", "order-service",   "Kafka Broker1",  "Kafka", "5ms",   "0.0%", "正常"},
            {"TOPO-NET", "core-router-01",  "core-switch-01", "L3",    "1ms",   "0.0%", "正常"},
            {"TOPO-NET", "core-switch-01",  "agg-switch-01",  "L2",    "1ms",   "0.0%", "正常"},
        };
        for (Object[] row : links) {
            topologyLinkRepository.insert(TopologyLink.builder()
                .topologyCode((String) row[0])
                .sourceNode((String) row[1])
                .targetNode((String) row[2])
                .linkType((String) row[3])
                .latency((String) row[4])
                .errorRate((String) row[5])
                .status((String) row[6])
                .build());
        }
    }

    private void seedBigScreens() {
        if (bigScreenRepository.count() > 0) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        Object[][] screens = {
            {"SCR-OPS-01",   "运维中心大屏",   "1920x1080", 16, 1, 1, 34},
            {"SCR-ALERT-01", "告警实时大屏",   "2560x1440", 10, 1, 1, 28},
            {"SCR-NET-01",   "网络拓扑大屏",   "3840x2160",  8, 0, 0, 12},
            {"SCR-K8S-01",   "容器云大屏",     "1920x1080", 14, 1, 1, 45},
            {"SCR-BIZ-01",   "业务指标大屏",   "2560x1440", 12, 1, 1, 67},
        };
        for (Object[] row : screens) {
            bigScreenRepository.insert(BigScreen.builder()
                .screenCode((String) row[0])
                .name((String) row[1])
                .resolution((String) row[2])
                .charts((Integer) row[3])
                .autoPlay((Integer) row[4])
                .shared((Integer) row[5])
                .visitToday((Integer) row[6])
                .updatedAt(now)
                .createdAt(now)
                .build());
        }
    }

    private void seedChartTemplates() {
        if (chartTemplateRepository.count() > 0) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        Object[][] templates = {
            {"TPL-LINE-01",   "时序折线图",     "折线图",   "通用",    248},
            {"TPL-BAR-01",    "柱状对比图",     "柱状图",   "通用",    186},
            {"TPL-GAUGE-01",  "仪表盘",         "仪表盘",   "监控",    312},
            {"TPL-PIE-01",    "饼图",           "饼图",     "通用",    124},
            {"TPL-TABLE-01",  "数据表格",       "表格",     "通用",    198},
            {"TPL-HEATMAP-01","热力图",         "热力图",   "监控",     87},
            {"TPL-TOPOLOGY-01","拓扑图模板",    "拓扑图",   "网络",     64},
            {"TPL-STAT-01",   "单值统计卡片",   "统计卡片", "通用",    421},
            {"TPL-ALERT-01",  "告警时序图",     "折线图",   "告警",    153},
            {"TPL-K8S-01",    "K8s资源视图",    "复合图",   "容器",     89},
        };
        for (Object[] row : templates) {
            chartTemplateRepository.insert(ChartTemplate.builder()
                .templateCode((String) row[0])
                .templateName((String) row[1])
                .chartType((String) row[2])
                .category((String) row[3])
                .usedCount((Integer) row[4])
                .createdAt(now)
                .updatedAt(now)
                .build());
        }
    }
}
