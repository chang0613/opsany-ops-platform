package com.opsany.replica.controller;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.opsany.replica.domain.AiConversation;
import com.opsany.replica.domain.AiJob;
import com.opsany.replica.domain.AiKnowledgeEntry;
import com.opsany.replica.domain.AiMessage;
import com.opsany.replica.security.AuthInterceptor;
import com.opsany.replica.security.SessionUser;
import com.opsany.replica.service.AiModuleService;
import com.opsany.replica.service.AiModuleService.AiRuntimeConfigUpdate;
import com.opsany.replica.service.AiModuleService.AiRuntimeConfigView;
import com.opsany.replica.service.AiModuleService.AiRuntimeTestResult;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiModuleService aiModuleService;

    @GetMapping("/config")
    public AiRuntimeConfigView getConfig() {
        return aiModuleService.getRuntimeConfig();
    }

    @PutMapping("/config")
    public AiRuntimeConfigView updateConfig(@RequestBody AiRuntimeConfigUpdate body) {
        return aiModuleService.updateRuntimeConfig(body);
    }

    @PostMapping("/config/test")
    public AiRuntimeTestResult testConfig(@RequestBody AiRuntimeConfigUpdate body) {
        return aiModuleService.testRuntimeConfig(body);
    }

    @GetMapping("/conversations")
    public List<AiConversation> listConversations(HttpServletRequest request) {
        return aiModuleService.listConversations(currentUser(request).getUsername());
    }

    @PostMapping("/conversations")
    public AiConversation createConversation(@RequestBody CreateConversationRequest body, HttpServletRequest request) {
        return aiModuleService.createConversation(currentUser(request).getUsername(), body == null ? null : body.getTitle());
    }

    @GetMapping("/conversations/{conversationId}/messages")
    public List<AiMessage> listMessages(@PathVariable("conversationId") long conversationId, HttpServletRequest request) {
        return aiModuleService.listMessages(conversationId, currentUser(request).getUsername());
    }

    @PostMapping("/conversations/{conversationId}/messages")
    public Map<String, Object> sendMessage(@PathVariable("conversationId") long conversationId, @RequestBody SendMessageRequest body,
        HttpServletRequest request) {
        AiMessage message = aiModuleService.sendMessage(conversationId, currentUser(request).getUsername(), body == null ? null : body.getContent());
        Map<String, Object> response = new HashMap<String, Object>();
        response.put("message", message);
        return response;
    }

    @PostMapping(path = "/conversations/{conversationId}/messages/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamMessage(@PathVariable("conversationId") long conversationId, @RequestBody SendMessageRequest body,
        HttpServletRequest request) {
        SseEmitter emitter = new SseEmitter(0L);
        SessionUser user = currentUser(request);
        String content = body == null ? null : body.getContent();

        new Thread(() -> {
            try {
                sendStep(emitter, "intent", "识别意图", "分析问题类型与可用工具", "in_progress");
                sendStep(emitter, "knowledge", "检索知识库", "召回历史故障经验与知识条目", "pending");
                sendStep(emitter, "context", "组装上下文", "拼接会话历史、知识片段和当前输入", "pending");
                sendStep(emitter, "tools", "执行工具策略", "根据问题内容调度相应工具能力", "pending");
                sendStep(emitter, "response", "流式生成回复", "按段返回最终答案", "pending");

                markStep(emitter, "intent");
                markStep(emitter, "knowledge");
                markStep(emitter, "context");
                markStep(emitter, "tools");
                markStep(emitter, "response");

                AiMessage message = aiModuleService.streamMessage(conversationId, user.getUsername(), content, delta -> sendDelta(emitter, delta));
                Map<String, Object> done = new HashMap<String, Object>();
                done.put("message", message);
                emitter.send(SseEmitter.event().name("done").data(done));
                emitter.complete();
            } catch (Exception exception) {
                try {
                    Map<String, Object> error = new HashMap<String, Object>();
                    error.put("message", exception.getMessage());
                    emitter.send(SseEmitter.event().name("error").data(error));
                } catch (IOException ignored) {
                    // Ignore secondary SSE failures.
                }
                emitter.completeWithError(exception);
            }
        }, "ai-stream-" + conversationId).start();

        return emitter;
    }

    @GetMapping("/jobs")
    public List<AiJob> listJobs(HttpServletRequest request) {
        return aiModuleService.listJobs(currentUser(request).getUsername());
    }

    @PostMapping("/jobs")
    public AiJob createJob(@RequestBody CreateJobRequest body, HttpServletRequest request) {
        String inputJson = body == null ? null : body.getInputJson();
        return aiModuleService.createJob(currentUser(request).getUsername(), body == null ? null : body.getJobType(), inputJson);
    }

    @GetMapping("/knowledge")
    public List<AiKnowledgeEntry> listKnowledge(HttpServletRequest request) {
        return aiModuleService.listKnowledge(currentUser(request).getUsername());
    }

    @PostMapping("/knowledge")
    public AiKnowledgeEntry createKnowledge(@RequestBody AiKnowledgeEntry payload, HttpServletRequest request) {
        return aiModuleService.createKnowledge(currentUser(request).getUsername(), payload);
    }

    @DeleteMapping("/knowledge/{id}")
    public void deleteKnowledge(@PathVariable("id") long id, HttpServletRequest request) {
        aiModuleService.deleteKnowledge(currentUser(request).getUsername(), id);
    }

    private void sendStep(SseEmitter emitter, String stepId, String title, String detail, String status) throws IOException {
        Map<String, Object> payload = new HashMap<String, Object>();
        payload.put("stepId", stepId);
        payload.put("title", title);
        payload.put("detail", detail);
        payload.put("status", status);
        emitter.send(SseEmitter.event().name("step").data(payload));
    }

    private void markStep(SseEmitter emitter, String currentStepId) throws IOException {
        Map<String, Object> payload = new HashMap<String, Object>();
        payload.put("stepId", currentStepId);
        emitter.send(SseEmitter.event().name("step-progress").data(payload));
    }

    private void sendDelta(SseEmitter emitter, String delta) {
        try {
            Map<String, Object> payload = new HashMap<String, Object>();
            payload.put("delta", delta);
            emitter.send(SseEmitter.event().name("delta").data(payload));
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to send stream delta", exception);
        }
    }

    private SessionUser currentUser(HttpServletRequest request) {
        return (SessionUser) request.getAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE);
    }

    @Data
    public static class CreateConversationRequest {
        private String title;
    }

    @Data
    public static class SendMessageRequest {
        private String content;
    }

    @Data
    public static class CreateJobRequest {
        private String jobType;
        private String inputJson;
    }
}
