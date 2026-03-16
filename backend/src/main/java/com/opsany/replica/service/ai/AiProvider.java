package com.opsany.replica.service.ai;

import java.util.List;

public interface AiProvider {

    interface StreamListener {
        void onDelta(String delta);
    }

    String getProviderCode();

    String chat(List<AiProviderMessage> messages);

    default String streamChat(List<AiProviderMessage> messages, StreamListener listener) {
        String content = chat(messages);
        if (listener != null && content != null) {
            listener.onDelta(content);
        }
        return content;
    }
}
