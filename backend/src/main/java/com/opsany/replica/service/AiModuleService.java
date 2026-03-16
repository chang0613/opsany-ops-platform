package com.opsany.replica.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opsany.replica.config.AppProperties;
import com.opsany.replica.domain.AiConversation;
import com.opsany.replica.domain.AiJob;
import com.opsany.replica.domain.AiKnowledgeEntry;
import com.opsany.replica.domain.AiMessage;
import com.opsany.replica.repository.AiConversationRepository;
import com.opsany.replica.repository.AiJobRepository;
import com.opsany.replica.repository.AiKnowledgeRepository;
import com.opsany.replica.repository.AiMessageRepository;
import com.opsany.replica.service.ai.AiProvider;
import com.opsany.replica.service.ai.AiProviderMessage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Service
@RequiredArgsConstructor
public class AiModuleService {

    public interface StreamObserver {
        void onDelta(String delta);
    }

    private static final String DEFAULT_CONVERSATION_TITLE = "新对话";

    private final AiConversationRepository aiConversationRepository;
    private final AiMessageRepository aiMessageRepository;
    private final AiJobRepository aiJobRepository;
    private final AiKnowledgeRepository aiKnowledgeRepository;
    private final List<AiProvider> providers;
    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;

    public List<AiConversation> listConversations(String ownerUsername) {
        return aiConversationRepository.findTop50ByOwnerOrderByUpdatedAtDesc(ownerUsername);
    }

    @Transactional
    public AiConversation createConversation(String ownerUsername, String title) {
        String normalized = normalizeConversationTitle(title);
        LocalDateTime now = LocalDateTime.now();
        AiConversation conversation = AiConversation.builder()
            .title(normalized)
            .ownerUsername(ownerUsername)
            .createdAt(now)
            .updatedAt(now)
            .build();
        aiConversationRepository.insert(conversation);

        aiMessageRepository.insert(AiMessage.builder()
            .conversationId(conversation.getId())
            .role("system")
            .content(defaultAssistantPrompt())
            .createdAt(now)
            .build());

        return conversation;
    }

    public List<AiMessage> listMessages(long conversationId, String ownerUsername) {
        AiConversation conversation = requireConversation(conversationId, ownerUsername);
        if (conversation == null) {
            throw new IllegalArgumentException("对话不存在或无权限访问");
        }
        return aiMessageRepository.findTop200ByConversationIdOrderByIdAsc(conversationId);
    }

    @Transactional
    public AiMessage sendMessage(long conversationId, String ownerUsername, String content) {
        PreparedConversation prepared = prepareConversation(conversationId, ownerUsername, content);
        String assistant = resolveProvider().chat(prepared.providerMessages);
        return persistAssistantReply(prepared, assistant);
    }

    @Transactional
    public AiMessage streamMessage(long conversationId, String ownerUsername, String content, StreamObserver observer) {
        PreparedConversation prepared = prepareConversation(conversationId, ownerUsername, content);
        String assistant = resolveProvider().streamChat(prepared.providerMessages, delta -> {
            if (observer != null && StringUtils.hasText(delta)) {
                observer.onDelta(delta);
            }
        });
        return persistAssistantReply(prepared, assistant);
    }

    public List<AiJob> listJobs(String ownerUsername) {
        return aiJobRepository.findTop50ByOwnerOrderByCreatedAtDesc(ownerUsername);
    }

    @Transactional
    public AiJob createJob(String ownerUsername, String jobType, String inputJson) {
        LocalDateTime now = LocalDateTime.now();
        String normalizedJobType = normalizeJobType(jobType);
        AiJob job = AiJob.builder()
            .jobType(normalizedJobType)
            .status("RUNNING")
            .ownerUsername(ownerUsername)
            .inputJson(inputJson)
            .resultJson(null)
            .createdAt(now)
            .updatedAt(now)
            .build();
        aiJobRepository.insert(job);

        try {
            String result = runInsightJob(ownerUsername, normalizedJobType, inputJson);
            String resultJson = buildJobResultJson(normalizedJobType, inputJson, result);
            aiJobRepository.updateResult(job.getId(), ownerUsername, "DONE", resultJson, LocalDateTime.now());
        } catch (Exception exception) {
            String failureJson = buildFailureResultJson(normalizedJobType, inputJson, exception.getMessage());
            aiJobRepository.updateResult(job.getId(), ownerUsername, "FAILED", failureJson, LocalDateTime.now());
        }

        return aiJobRepository.findById(job.getId());
    }

    public List<AiKnowledgeEntry> listKnowledge(String ownerUsername) {
        return aiKnowledgeRepository.findTop50ByOwnerOrderByCreatedAtDesc(ownerUsername);
    }

    @Transactional
    public AiKnowledgeEntry createKnowledge(String ownerUsername, AiKnowledgeEntry payload) {
        LocalDateTime now = LocalDateTime.now();
        AiKnowledgeEntry entry = AiKnowledgeEntry.builder()
            .title(payload.getTitle() == null ? "" : payload.getTitle().trim())
            .content(payload.getContent() == null ? "" : payload.getContent().trim())
            .tags(payload.getTags())
            .sourceType(payload.getSourceType())
            .sourceId(payload.getSourceId())
            .ownerUsername(ownerUsername)
            .createdAt(now)
            .updatedAt(now)
            .build();
        if (entry.getTitle().isEmpty() || entry.getContent().isEmpty()) {
            throw new IllegalArgumentException("标题和内容不能为空");
        }
        aiKnowledgeRepository.insert(entry);
        return entry;
    }

    @Transactional
    public void deleteKnowledge(String ownerUsername, long id) {
        aiKnowledgeRepository.deleteByIdAndOwner(id, ownerUsername);
    }

    public AiRuntimeConfigView getRuntimeConfig() {
        AppProperties.Ai ai = appProperties.getAi();
        AppProperties.BigModel bigModel = ai.getBigmodel();
        return AiRuntimeConfigView.builder()
            .provider(ai.getProvider())
            .knowledgeContextEnabled(ai.isKnowledgeContextEnabled())
            .configured(isBigModelConfigured())
            .apiKeyConfigured(StringUtils.hasText(bigModel.getApiKey()))
            .apiKeyMasked(maskApiKey(bigModel.getApiKey()))
            .baseUrl(bigModel.getBaseUrl())
            .model(bigModel.getAgentId())
            .connectTimeoutMs(bigModel.getConnectTimeoutMs())
            .readTimeoutMs(bigModel.getReadTimeoutMs())
            .build();
    }

    public AiRuntimeConfigView updateRuntimeConfig(AiRuntimeConfigUpdate update) {
        applyConfig(update, true);
        return getRuntimeConfig();
    }

    public AiRuntimeTestResult testRuntimeConfig(AiRuntimeConfigUpdate update) {
        RuntimeSnapshot snapshot = snapshotConfig();
        try {
            applyConfig(update, false);
            List<AiProviderMessage> messages = new ArrayList<AiProviderMessage>();
            messages.add(new AiProviderMessage("system", defaultAssistantPrompt()));
            messages.add(new AiProviderMessage("user", "请仅返回“连接成功”四个字，用于验证模型连通性。"));
            String response = resolveProvider().chat(messages);
            return AiRuntimeTestResult.builder()
                .success(StringUtils.hasText(response))
                .message(StringUtils.hasText(response) ? "连接成功，可直接开始使用。" : "模型未返回有效内容。")
                .preview(previewText(response))
                .build();
        } catch (Exception exception) {
            return AiRuntimeTestResult.builder()
                .success(false)
                .message(exception.getMessage())
                .preview("")
                .build();
        } finally {
            restoreConfig(snapshot);
        }
    }

    private PreparedConversation prepareConversation(long conversationId, String ownerUsername, String content) {
        AiConversation conversation = requireConversation(conversationId, ownerUsername);
        String normalized = content == null ? "" : content.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("消息内容不能为空");
        }

        LocalDateTime now = LocalDateTime.now();
        aiMessageRepository.insert(AiMessage.builder()
            .conversationId(conversationId)
            .role("user")
            .content(normalized)
            .createdAt(now)
            .build());

        List<AiMessage> history = aiMessageRepository.findTop200ByConversationIdOrderByIdAsc(conversationId);
        List<AiProviderMessage> providerMessages = buildConversationMessages(ownerUsername, history, normalized);
        return new PreparedConversation(conversation, normalized, providerMessages);
    }

    private AiMessage persistAssistantReply(PreparedConversation prepared, String assistantContent) {
        String normalizedAssistant = StringUtils.hasText(assistantContent) ? assistantContent.trim() : "未返回有效内容。";
        AiMessage assistantMessage = AiMessage.builder()
            .conversationId(prepared.conversation.getId())
            .role("assistant")
            .content(normalizedAssistant)
            .createdAt(LocalDateTime.now())
            .build();
        aiMessageRepository.insert(assistantMessage);

        String finalTitle = shouldAutoRename(prepared.conversation.getTitle())
            ? generateConversationTitle(prepared.userContent)
            : prepared.conversation.getTitle();
        aiConversationRepository.updateTitle(prepared.conversation.getId(), prepared.conversation.getOwnerUsername(), finalTitle, LocalDateTime.now());
        return assistantMessage;
    }

    private List<AiProviderMessage> buildConversationMessages(String ownerUsername, List<AiMessage> history, String query) {
        List<AiProviderMessage> providerMessages = new ArrayList<AiProviderMessage>();
        providerMessages.add(new AiProviderMessage("system", defaultAssistantPrompt()));

        String knowledgeContext = buildKnowledgeContext(ownerUsername, query);
        if (StringUtils.hasText(knowledgeContext)) {
            providerMessages.add(new AiProviderMessage("system", knowledgeContext));
        }

        providerMessages.addAll(history.stream()
            .filter(item -> item != null && StringUtils.hasText(item.getContent()))
            .map(item -> new AiProviderMessage(item.getRole(), item.getContent()))
            .collect(Collectors.toList()));
        return providerMessages;
    }

    private void applyConfig(AiRuntimeConfigUpdate update, boolean keepExistingWhenBlank) {
        if (update == null) {
            return;
        }

        AppProperties.Ai ai = appProperties.getAi();
        AppProperties.BigModel bigModel = ai.getBigmodel();

        if (StringUtils.hasText(update.getProvider())) {
            ai.setProvider(update.getProvider().trim());
        }
        if (update.getKnowledgeContextEnabled() != null) {
            ai.setKnowledgeContextEnabled(update.getKnowledgeContextEnabled().booleanValue());
        }
        if (StringUtils.hasText(update.getBaseUrl())) {
            bigModel.setBaseUrl(update.getBaseUrl().trim());
        }
        if (StringUtils.hasText(update.getModel())) {
            bigModel.setAgentId(update.getModel().trim());
        }
        if (update.getConnectTimeoutMs() != null && update.getConnectTimeoutMs().longValue() > 0) {
            bigModel.setConnectTimeoutMs(update.getConnectTimeoutMs().longValue());
        }
        if (update.getReadTimeoutMs() != null && update.getReadTimeoutMs().longValue() > 0) {
            bigModel.setReadTimeoutMs(update.getReadTimeoutMs().longValue());
        }
        if (Boolean.TRUE.equals(update.getClearApiKey())) {
            bigModel.setApiKey(null);
        } else if (update.getApiKey() != null) {
            String normalizedApiKey = update.getApiKey().trim();
            if (StringUtils.hasText(normalizedApiKey)) {
                bigModel.setApiKey(normalizedApiKey);
            } else if (!keepExistingWhenBlank) {
                bigModel.setApiKey(null);
            }
        }

        if (!StringUtils.hasText(ai.getProvider())) {
            ai.setProvider(isBigModelConfigured() ? "bigmodel" : "mock");
        }
    }

    private RuntimeSnapshot snapshotConfig() {
        AppProperties.Ai ai = appProperties.getAi();
        AppProperties.BigModel bigModel = ai.getBigmodel();
        return new RuntimeSnapshot(
            ai.getProvider(),
            ai.isKnowledgeContextEnabled(),
            bigModel.getApiKey(),
            bigModel.getBaseUrl(),
            bigModel.getAgentId(),
            bigModel.getConnectTimeoutMs(),
            bigModel.getReadTimeoutMs()
        );
    }

    private void restoreConfig(RuntimeSnapshot snapshot) {
        AppProperties.Ai ai = appProperties.getAi();
        AppProperties.BigModel bigModel = ai.getBigmodel();
        ai.setProvider(snapshot.provider);
        ai.setKnowledgeContextEnabled(snapshot.knowledgeContextEnabled);
        bigModel.setApiKey(snapshot.apiKey);
        bigModel.setBaseUrl(snapshot.baseUrl);
        bigModel.setAgentId(snapshot.model);
        bigModel.setConnectTimeoutMs(snapshot.connectTimeoutMs);
        bigModel.setReadTimeoutMs(snapshot.readTimeoutMs);
    }

    private boolean isBigModelConfigured() {
        AppProperties.BigModel bigModel = appProperties.getAi().getBigmodel();
        return StringUtils.hasText(bigModel.getApiKey())
            && StringUtils.hasText(bigModel.getBaseUrl())
            && StringUtils.hasText(bigModel.getAgentId());
    }

    private String maskApiKey(String apiKey) {
        if (!StringUtils.hasText(apiKey)) {
            return "";
        }
        String normalized = apiKey.trim();
        if (normalized.length() <= 8) {
            return "已配置";
        }
        return normalized.substring(0, 4) + "****" + normalized.substring(normalized.length() - 4);
    }

    private String previewText(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String normalized = value.replaceAll("\\s+", " ").trim();
        return normalized.length() <= 120 ? normalized : normalized.substring(0, 120) + "...";
    }

    private String runInsightJob(String ownerUsername, String jobType, String inputJson) {
        List<AiProviderMessage> messages = new ArrayList<AiProviderMessage>();
        messages.add(new AiProviderMessage("system", defaultAssistantPrompt()));

        String knowledgeContext = buildKnowledgeContext(ownerUsername, inputJson);
        if (StringUtils.hasText(knowledgeContext)) {
            messages.add(new AiProviderMessage("system", knowledgeContext));
        }

        messages.add(new AiProviderMessage("user", buildJobPrompt(jobType, inputJson)));
        return resolveProvider().chat(messages);
    }

    private String buildJobPrompt(String jobType, String inputJson) {
        String payload = StringUtils.hasText(inputJson) ? inputJson.trim() : defaultJobInput(jobType);
        if ("alarm_summary".equals(jobType)) {
            return "请作为监控智能分析助手，基于以下告警与上下文生成故障摘要。输出需要包含：故障现象、影响范围、时间线、优先排查方向、止血建议。上下文：\n" + payload;
        }
        if ("root_cause".equals(jobType)) {
            return "请对以下监控、日志和变更上下文做根因分析。输出 Top3 根因候选、每条根因的证据、验证动作和建议处理顺序。上下文：\n" + payload;
        }
        if ("log_cluster".equals(jobType)) {
            return "请对以下日志样本做语义聚类，输出：事件模式、共性特征、异常模式、建议标签和可能关联组件。日志样本：\n" + payload;
        }
        if ("capacity_plan".equals(jobType)) {
            return "请根据以下资源趋势和业务背景生成容量规划报告。输出：趋势判断、风险时点、扩容建议、优化建议。上下文：\n" + payload;
        }
        if ("change_impact".equals(jobType)) {
            return "请根据以下配置项和变更内容进行影响范围分析。输出：受影响对象、风险等级、冲突点、回滚建议、验证清单。上下文：\n" + payload;
        }
        if ("ticket_dispatch".equals(jobType)) {
            return "请根据以下工单内容做智能分类与派单。输出：工单类型、紧急程度、推荐处理队列、推荐责任人画像、补充信息建议。工单内容：\n" + payload;
        }
        if ("ticket_solution".equals(jobType)) {
            return "请根据以下工单与历史上下文给出解决方案推荐。输出：推荐步骤、风险提示、验证动作、是否适合自动化。工单内容：\n" + payload;
        }
        if ("config_normalize".equals(jobType)) {
            return "请对以下资源配置数据做智能标准化与纠错建议。输出：标准化结果、缺失项补全建议、异常值、潜在关联关系。数据：\n" + payload;
        }
        return "请作为 OpsAny AI 中枢，针对以下输入生成可执行分析结果，输出重点结论、证据和后续动作。输入：\n" + payload;
    }

    private String buildJobResultJson(String jobType, String inputJson, String summary) {
        try {
            Map<String, Object> payload = new LinkedHashMap<String, Object>();
            payload.put("jobType", jobType);
            payload.put("summary", summary);
            payload.put("input", safeInputNode(inputJson));
            payload.put("generatedAt", LocalDateTime.now().toString());
            return objectMapper.writeValueAsString(payload);
        } catch (Exception exception) {
            return "{\"jobType\":\"" + jobType + "\",\"summary\":\"" + escapeJson(summary) + "\"}";
        }
    }

    private String buildFailureResultJson(String jobType, String inputJson, String message) {
        try {
            Map<String, Object> payload = new LinkedHashMap<String, Object>();
            payload.put("jobType", jobType);
            payload.put("error", message);
            payload.put("input", safeInputNode(inputJson));
            payload.put("generatedAt", LocalDateTime.now().toString());
            return objectMapper.writeValueAsString(payload);
        } catch (Exception exception) {
            return "{\"jobType\":\"" + jobType + "\",\"error\":\"" + escapeJson(message) + "\"}";
        }
    }

    private Object safeInputNode(String inputJson) {
        if (!StringUtils.hasText(inputJson)) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readTree(inputJson);
        } catch (Exception ignored) {
            return inputJson;
        }
    }

    private String buildKnowledgeContext(String ownerUsername, String query) {
        if (!appProperties.getAi().isKnowledgeContextEnabled() || !StringUtils.hasText(query)) {
            return "";
        }

        List<AiKnowledgeEntry> entries = listKnowledge(ownerUsername);
        if (entries == null || entries.isEmpty()) {
            return "";
        }

        String normalizedQuery = query.toLowerCase(Locale.ROOT);
        List<AiKnowledgeEntry> matches = entries.stream()
            .sorted(Comparator.comparingInt((AiKnowledgeEntry entry) -> scoreKnowledge(entry, normalizedQuery)).reversed())
            .filter(entry -> scoreKnowledge(entry, normalizedQuery) > 0)
            .limit(3)
            .collect(Collectors.toList());

        if (matches.isEmpty()) {
            matches = entries.stream().limit(3).collect(Collectors.toList());
        }

        StringBuilder builder = new StringBuilder();
        builder.append("以下是可供参考的内部知识库，请优先结合这些内容进行回答，不要编造不存在的内部事实：\n");
        for (AiKnowledgeEntry entry : matches) {
            builder.append("- 标题: ").append(nullToEmpty(entry.getTitle())).append('\n');
            if (StringUtils.hasText(entry.getTags())) {
                builder.append("  标签: ").append(entry.getTags().trim()).append('\n');
            }
            builder.append("  内容: ").append(nullToEmpty(entry.getContent())).append("\n");
        }
        return builder.toString().trim();
    }

    private int scoreKnowledge(AiKnowledgeEntry entry, String query) {
        int score = 0;
        score += scoreField(entry.getTitle(), query, 5);
        score += scoreField(entry.getTags(), query, 3);
        score += scoreField(entry.getContent(), query, 1);
        return score;
    }

    private int scoreField(String field, String query, int weight) {
        if (!StringUtils.hasText(field) || !StringUtils.hasText(query)) {
            return 0;
        }
        String normalizedField = field.toLowerCase(Locale.ROOT);
        int score = 0;
        if (query.length() >= 2 && normalizedField.contains(query)) {
            score += weight * 3;
        }
        for (String token : query.split("[\\s,;，；]+")) {
            if (token.length() < 2) {
                continue;
            }
            if (normalizedField.contains(token)) {
                score += weight;
            }
        }
        return score;
    }

    private AiProvider resolveProvider() {
        if (providers == null || providers.isEmpty()) {
            throw new IllegalStateException("未配置 AI Provider");
        }

        String preferredCode = appProperties.getAi().getProvider();
        if (StringUtils.hasText(preferredCode)) {
            for (AiProvider provider : providers) {
                if (preferredCode.equalsIgnoreCase(provider.getProviderCode())) {
                    return provider;
                }
            }
        }

        for (AiProvider provider : providers) {
            if ("mock".equalsIgnoreCase(provider.getProviderCode())) {
                return provider;
            }
        }
        return providers.get(0);
    }

    private AiConversation requireConversation(long conversationId, String ownerUsername) {
        AiConversation conversation = aiConversationRepository.findById(conversationId);
        if (conversation == null || !ownerUsername.equals(conversation.getOwnerUsername())) {
            throw new IllegalArgumentException("对话不存在或无权限访问");
        }
        return conversation;
    }

    private String normalizeConversationTitle(String title) {
        if (!StringUtils.hasText(title)) {
            return DEFAULT_CONVERSATION_TITLE;
        }
        return title.trim();
    }

    private boolean shouldAutoRename(String title) {
        return !StringUtils.hasText(title)
            || DEFAULT_CONVERSATION_TITLE.equals(title.trim())
            || "运维助手对话".equals(title.trim());
    }

    private String generateConversationTitle(String content) {
        String normalized = content == null ? "" : content.replaceAll("\\s+", " ").trim();
        if (normalized.isEmpty()) {
            return DEFAULT_CONVERSATION_TITLE;
        }
        if (normalized.length() <= 18) {
            return normalized;
        }
        return normalized.substring(0, 18) + "...";
    }

    private String normalizeJobType(String jobType) {
        if (!StringUtils.hasText(jobType)) {
            return "generic_analysis";
        }
        return jobType.trim();
    }

    private String defaultAssistantPrompt() {
        return "你是 OpsAny AI 助手，服务于监控管理、资源配置管理和工单管理场景。请优先输出结构化、可执行、可验证的建议；涉及未知内部信息时要明确说明依据不足。";
    }

    private String defaultJobInput(String jobType) {
        if ("alarm_summary".equals(jobType)) {
            return "{\"alerts\":[{\"name\":\"mysql connection high\",\"level\":\"P1\",\"value\":92,\"time\":\"2026-03-12 10:00:00\"}],\"scope\":\"production\"}";
        }
        if ("root_cause".equals(jobType)) {
            return "{\"metric\":\"api p99 latency\",\"trend\":\"up\",\"recentChanges\":[\"release-2026.03.12\"],\"logs\":[\"timeout from db\"]}";
        }
        if ("log_cluster".equals(jobType)) {
            return "{\"logs\":[\"ERROR connect timeout to redis\",\"WARN redis response slow\",\"ERROR connect timeout to mysql\"]}";
        }
        if ("capacity_plan".equals(jobType)) {
            return "{\"resource\":\"k8s cluster cpu\",\"last30d\":[62,64,66,70,74],\"holiday\":false}";
        }
        if ("change_impact".equals(jobType)) {
            return "{\"ci\":\"prod-nginx-01\",\"change\":\"upgrade nginx 1.26\",\"dependencies\":[\"gateway\",\"order-api\"]}";
        }
        if ("ticket_dispatch".equals(jobType) || "ticket_solution".equals(jobType)) {
            return "{\"title\":\"生产环境订单接口超时\",\"description\":\"用户反馈下单失败，接口响应超过 10 秒\",\"priority\":\"high\"}";
        }
        if ("config_normalize".equals(jobType)) {
            return "{\"hostname\":\"Prod-Redis-01\",\"cpu\":\"Cpu\",\"owner\":\"ops\",\"ip\":\"10.0.0.8\"}";
        }
        return "{\"note\":\"describe your scenario here\"}";
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private String escapeJson(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }

    private static class PreparedConversation {
        private final AiConversation conversation;
        private final String userContent;
        private final List<AiProviderMessage> providerMessages;

        private PreparedConversation(AiConversation conversation, String userContent, List<AiProviderMessage> providerMessages) {
            this.conversation = conversation;
            this.userContent = userContent;
            this.providerMessages = providerMessages;
        }
    }

    private static class RuntimeSnapshot {
        private final String provider;
        private final boolean knowledgeContextEnabled;
        private final String apiKey;
        private final String baseUrl;
        private final String model;
        private final long connectTimeoutMs;
        private final long readTimeoutMs;

        private RuntimeSnapshot(String provider, boolean knowledgeContextEnabled, String apiKey, String baseUrl, String model,
            long connectTimeoutMs, long readTimeoutMs) {
            this.provider = provider;
            this.knowledgeContextEnabled = knowledgeContextEnabled;
            this.apiKey = apiKey;
            this.baseUrl = baseUrl;
            this.model = model;
            this.connectTimeoutMs = connectTimeoutMs;
            this.readTimeoutMs = readTimeoutMs;
        }
    }

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    public static class AiRuntimeConfigView {
        private String provider;
        private boolean knowledgeContextEnabled;
        private boolean configured;
        private boolean apiKeyConfigured;
        private String apiKeyMasked;
        private String baseUrl;
        private String model;
        private long connectTimeoutMs;
        private long readTimeoutMs;
    }

    @Getter
    @Setter
    public static class AiRuntimeConfigUpdate {
        private String provider;
        private Boolean knowledgeContextEnabled;
        private String apiKey;
        private Boolean clearApiKey;
        private String baseUrl;
        private String model;
        private Long connectTimeoutMs;
        private Long readTimeoutMs;
    }

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    public static class AiRuntimeTestResult {
        private boolean success;
        private String message;
        private String preview;
    }
}
