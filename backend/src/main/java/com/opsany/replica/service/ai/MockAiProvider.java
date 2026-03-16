package com.opsany.replica.service.ai;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Component;

@Component
public class MockAiProvider implements AiProvider {

    @Override
    public String getProviderCode() {
        return "mock";
    }

    @Override
    public String chat(List<AiProviderMessage> messages) {
        String lastUser = "";
        for (int i = messages.size() - 1; i >= 0; i--) {
            AiProviderMessage msg = messages.get(i);
            if ("user".equalsIgnoreCase(msg.getRole())) {
                lastUser = msg.getContent();
                break;
            }
        }

        String normalized = lastUser == null ? "" : lastUser.trim();
        if (normalized.isEmpty()) {
            return "我已进入 AI 助手（Mock Provider）模式。你可以描述告警、日志、工单或配置问题，我会给出排查思路与建议动作。";
        }

        return "（Mock AI）我收到了你的问题：\n\n"
            + normalized
            + "\n\n"
            + "建议的下一步：\n"
            + "1. 明确影响范围，包括时间、环境、系统和资源。\n"
            + "2. 关联指标、日志、工单和变更，补齐时间线。\n"
            + "3. 给出 3 条最可能的根因假设，并逐条验证。\n\n"
            + "生成时间：" + LocalDateTime.now();
    }

    @Override
    public String streamChat(List<AiProviderMessage> messages, StreamListener listener) {
        String content = chat(messages);
        if (listener == null || content == null || content.isEmpty()) {
            return content;
        }

        for (String chunk : content.split("(?<=\\G.{24})")) {
            if (!chunk.isEmpty()) {
                listener.onDelta(chunk);
            }
        }
        return content;
    }
}
