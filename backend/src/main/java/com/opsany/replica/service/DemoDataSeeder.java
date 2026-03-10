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
import com.opsany.replica.domain.DutyGroup;
import com.opsany.replica.domain.DutyShift;
import com.opsany.replica.domain.MenuPermission;
import com.opsany.replica.domain.MessageSubscription;
import com.opsany.replica.domain.NotificationMessage;
import com.opsany.replica.domain.PlatformNavigationGroup;
import com.opsany.replica.domain.PlatformNavigationItem;
import com.opsany.replica.domain.TaskRecord;
import com.opsany.replica.domain.WorkOrder;
import com.opsany.replica.domain.WorkOrderCatalog;
import com.opsany.replica.domain.WorkOrderProcessDefinition;
import com.opsany.replica.domain.WorkOrderProcessNode;
import com.opsany.replica.repository.AppUserRepository;
import com.opsany.replica.repository.DutyScheduleRepository;
import com.opsany.replica.repository.MenuRepository;
import com.opsany.replica.repository.MessageSubscriptionRepository;
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
            menu("SYSTEM_SETTING", "平台设置", 5, "系统设置", "/serveSetting/systemSetting", "系", 2, "system:view")
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
}
