package com.opsany.replica.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import javax.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PlatformShellTemplateService {

    private final ObjectMapper objectMapper;
    private final PlatformTemplateService platformTemplateService;

    private ObjectNode shellTemplates;

    @PostConstruct
    void loadTemplateShells() throws IOException {
        ClassPathResource resource = new ClassPathResource("seed/platform-shells.json");
        try (InputStream inputStream = resource.getInputStream()) {
            shellTemplates = (ObjectNode) objectMapper.readTree(inputStream);
        }
    }

    public ObjectNode copyTemplate(String platformKey, String platformName, String platformDescription, String basePath) {
        if ("workbench".equals(platformKey)) {
            ObjectNode template = platformTemplateService.copyTemplate();
            applyShellMetadata(template, platformKey, defaultIfBlank(platformName, "工作台"), basePath);
            return template;
        }

        JsonNode template = shellTemplates == null ? null : shellTemplates.get(platformKey);
        if (template instanceof ObjectNode) {
            ObjectNode copy = ((ObjectNode) template).deepCopy();
            applyShellMetadata(copy, platformKey, defaultIfBlank(platformName, copy.path("shell").path("productName").asText("平台")), basePath);
            if (copy.with("pages").has("/")) {
                ObjectNode rootPage = (ObjectNode) copy.with("pages").get("/");
                rootPage.put("intro", defaultIfBlank(rootPage.path("intro").asText(), platformDescription));
            }
            return copy;
        }

        return buildGenericTemplate(platformKey, platformName, platformDescription, basePath);
    }

    private ObjectNode buildGenericTemplate(String platformKey, String platformName, String platformDescription, String basePath) {
        String displayName = defaultIfBlank(platformName, "平台");
        String normalizedBasePath = normalizeBasePath(basePath);
        ObjectNode root = objectMapper.createObjectNode();

        ObjectNode shell = root.putObject("shell");
        shell.put("productName", displayName);
        shell.put("platformButton", "平台导航");
        ArrayNode topNav = shell.putArray("topNav");
        Arrays.asList("控制台", "工作台", "消息", "支持").forEach(topNav::add);
        shell.put("basePath", normalizedBasePath);
        shell.put("platformKey", defaultIfBlank(platformKey, "generic"));
        ObjectNode user = shell.putObject("user");
        user.put("account", "demo");
        user.put("displayName", "演示用户");

        ArrayNode menu = root.putArray("menu");
        menu.add(menuGroup("", menuItem("概览", "/")));
        menu.add(menuGroup("功能中心",
            menuItem("产品概览", "/overview"),
            menuItem("能力矩阵", "/capability"),
            menuItem("接入配置", "/settings")
        ));

        root.putObject("overviewData");
        ObjectNode pages = root.putObject("pages");
        pages.set("/", genericPage(
            "概览",
            displayName,
            displayName + " 的平台概览页，当前已切换到独立菜单体系，可在这里继续补足具体业务能力。",
            "platformOverview",
            new String[][] {
                {"平台能力", "3", "概览 / 能力矩阵 / 配置"},
                {"导航来源", "平台导航", "从顶部面板进入"},
                {"当前平台", displayName, "与工作台左侧菜单已分离"}
            },
            new String[][] {
                {displayName + " 概览", defaultIfBlank(platformDescription, displayName + " 平台"), "已切换独立壳", "green", "/overview"},
                {"能力矩阵", "继续在这个平台下扩展子功能、配置项和管理页面。", "推荐继续补细节", "blue", "/capability"}
            }
        ));
        pages.set("/overview", modulePage("产品概览", displayName, "查看当前平台的基础说明、接入方式和能力说明。"));
        pages.set("/capability", modulePage("能力矩阵", displayName, "通过卡片和表格继续补充该平台的真功能。"));
        pages.set("/settings", modulePage("接入配置", displayName, "维护当前平台的接入配置和协作参数。"));
        return root;
    }

    private void applyShellMetadata(ObjectNode root, String platformKey, String platformName, String basePath) {
        ObjectNode shell = root.with("shell");
        shell.put("productName", defaultIfBlank(platformName, shell.path("productName").asText("平台")));
        shell.put("platformButton", defaultIfBlank(shell.path("platformButton").asText(), "平台导航"));
        shell.put("basePath", normalizeBasePath(basePath));
        shell.put("platformKey", defaultIfBlank(platformKey, shell.path("platformKey").asText("workbench")));
        if (!shell.has("topNav") || !shell.get("topNav").isArray()) {
            ArrayNode topNav = shell.putArray("topNav");
            Arrays.asList("控制台", "工作台", "消息", "支持").forEach(topNav::add);
        }
    }

    private ObjectNode menuGroup(String group, ObjectNode... items) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("group", group);
        ArrayNode array = node.putArray("items");
        for (ObjectNode item : items) {
            array.add(item);
        }
        return node;
    }

    private ObjectNode menuItem(String label, String route) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("label", label);
        node.put("route", route);
        return node;
    }

    private ObjectNode genericPage(
        String title,
        String platformName,
        String intro,
        String kind,
        String[][] stats,
        String[][] cards
    ) {
        ObjectNode page = page(title, platformName, intro, kind);
        ArrayNode statArray = page.putArray("stats");
        for (String[] stat : stats) {
            ObjectNode node = statArray.addObject();
            node.put("label", stat[0]);
            node.put("value", stat[1]);
            node.put("hint", stat[2]);
        }
        ArrayNode cardArray = page.putArray("cards");
        for (String[] card : cards) {
            ObjectNode node = cardArray.addObject();
            node.put("title", card[0]);
            node.put("desc", card[1]);
            node.put("meta", card[2]);
            node.put("accent", card[3]);
            node.put("route", card[4]);
        }
        return page;
    }

    private ObjectNode modulePage(String title, String platformName, String intro) {
        ObjectNode page = page(title, platformName, intro, "platformModule");
        ArrayNode cards = page.putArray("cards");
        ObjectNode card = cards.addObject();
        card.put("title", title);
        card.put("desc", intro);
        card.put("meta", "可继续扩展为真实业务页面");
        card.put("accent", "green");
        return page;
    }

    private ObjectNode page(String title, String platformName, String intro, String kind) {
        ObjectNode page = objectMapper.createObjectNode();
        page.put("title", title);
        ArrayNode breadcrumb = page.putArray("breadcrumb");
        breadcrumb.add(platformName);
        breadcrumb.add(title);
        page.put("intro", intro);
        page.put("kind", kind);
        return page;
    }

    private String defaultIfBlank(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }

    private String normalizeBasePath(String value) {
        if (value == null || value.trim().isEmpty() || "/".equals(value.trim())) {
            return "/o/workbench";
        }
        String normalized = value.trim();
        if (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized.isEmpty() ? "/o/workbench" : normalized;
    }
}
