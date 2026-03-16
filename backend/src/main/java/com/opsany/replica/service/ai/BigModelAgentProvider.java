package com.opsany.replica.service.ai;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.opsany.replica.config.AppProperties;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class BigModelAgentProvider implements AiProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(BigModelAgentProvider.class);

    private final AppProperties appProperties;
    private final RestTemplateBuilder restTemplateBuilder;
    private final ObjectMapper objectMapper;

    @Override
    public String getProviderCode() {
        return "bigmodel";
    }

    @Override
    public String chat(List<AiProviderMessage> messages) {
        AppProperties.BigModel config = appProperties.getAi().getBigmodel();
        ensureApiKey(config);

        try {
            RestTemplate restTemplate = createRestTemplate(config);
            HttpEntity<String> entity = new HttpEntity<String>(buildPayload(messages, false), buildHeaders(config));
            ResponseEntity<String> response = restTemplate.postForEntity(config.getBaseUrl(), entity, String.class);
            return extractContent(response.getBody());
        } catch (HttpStatusCodeException exception) {
            LOGGER.warn("BigModel API request failed: status={}, body={}", exception.getRawStatusCode(), exception.getResponseBodyAsString());
            throw new IllegalStateException(readErrorMessage(exception.getResponseBodyAsString(), exception.getStatusText()));
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to call BigModel Agent API: " + exception.getMessage(), exception);
        }
    }

    @Override
    public String streamChat(List<AiProviderMessage> messages, StreamListener listener) {
        AppProperties.BigModel config = appProperties.getAi().getBigmodel();
        ensureApiKey(config);

        StringBuilder builder = new StringBuilder();
        try {
            RestTemplate restTemplate = createRestTemplate(config);
            RequestEntity<String> request = RequestEntity
                .post(URI.create(config.getBaseUrl()))
                .headers(buildHeaders(config))
                .contentType(MediaType.APPLICATION_JSON)
                .body(buildPayload(messages, true));

            restTemplate.execute(request.getUrl(), HttpMethod.POST, clientHttpRequest -> {
                clientHttpRequest.getHeaders().putAll(request.getHeaders());
                String requestBody = request.getBody() == null ? "" : request.getBody();
                clientHttpRequest.getBody().write(requestBody.getBytes(StandardCharsets.UTF_8));
            }, clientHttpResponse -> {
                try (InputStream body = clientHttpResponse.getBody();
                     BufferedReader reader = new BufferedReader(new InputStreamReader(body, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String trimmed = line.trim();
                        if (!trimmed.startsWith("data:")) {
                            continue;
                        }

                        String data = trimmed.substring(5).trim();
                        if (!StringUtils.hasText(data)) {
                            continue;
                        }
                        if ("[DONE]".equals(data)) {
                            break;
                        }

                        String delta = extractStreamDelta(data);
                        if (!StringUtils.hasText(delta)) {
                            continue;
                        }
                        builder.append(delta);
                        if (listener != null) {
                            listener.onDelta(delta);
                        }
                    }
                }
                return null;
            });
            return builder.toString();
        } catch (HttpStatusCodeException exception) {
            LOGGER.warn("BigModel stream request failed: status={}, body={}", exception.getRawStatusCode(), exception.getResponseBodyAsString());
            throw new IllegalStateException(readErrorMessage(exception.getResponseBodyAsString(), exception.getStatusText()));
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to stream from BigModel Agent API: " + exception.getMessage(), exception);
        }
    }

    private void ensureApiKey(AppProperties.BigModel config) {
        if (!StringUtils.hasText(config.getApiKey())) {
            throw new IllegalStateException("BIGMODEL_API_KEY is not configured");
        }
    }

    private RestTemplate createRestTemplate(AppProperties.BigModel config) {
        return restTemplateBuilder
            .setConnectTimeout(Duration.ofMillis(config.getConnectTimeoutMs()))
            .setReadTimeout(Duration.ofMillis(config.getReadTimeoutMs()))
            .build();
    }

    private HttpHeaders buildHeaders(AppProperties.BigModel config) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Arrays.asList(MediaType.TEXT_EVENT_STREAM, MediaType.APPLICATION_JSON));
        headers.setBearerAuth(config.getApiKey().trim());
        return headers;
    }

    private String buildPayload(List<AiProviderMessage> messages, boolean stream) throws Exception {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("agent_id", appProperties.getAi().getBigmodel().getAgentId());
        payload.put("stream", stream);

        ArrayNode messageArray = payload.putArray("messages");
        for (AiProviderMessage message : messages) {
            if (message == null || !StringUtils.hasText(message.getContent())) {
                continue;
            }
            ObjectNode node = messageArray.addObject();
            node.put("role", normalizeRole(message.getRole()));
            node.put("content", message.getContent().trim());
        }
        return objectMapper.writeValueAsString(payload);
    }

    private String normalizeRole(String role) {
        if ("assistant".equalsIgnoreCase(role)) {
            return "assistant";
        }
        if ("system".equalsIgnoreCase(role)) {
            return "system";
        }
        return "user";
    }

    private String extractContent(String body) throws Exception {
        if (!StringUtils.hasText(body)) {
            throw new IllegalStateException("BigModel Agent API returned an empty body");
        }

        JsonNode root = objectMapper.readTree(body);
        JsonNode contentNode = root.path("choices").path(0).path("message").path("content");
        if (!contentNode.isMissingNode() && !contentNode.isNull()) {
            return normalizeContent(contentNode);
        }

        JsonNode fallbackNode = root.path("choices").path(0).path("content");
        if (!fallbackNode.isMissingNode() && !fallbackNode.isNull()) {
            return normalizeContent(fallbackNode);
        }

        throw new IllegalStateException(readErrorMessage(body, "Unable to extract assistant content from BigModel response"));
    }

    private String extractStreamDelta(String body) throws Exception {
        JsonNode root = objectMapper.readTree(body);
        JsonNode deltaNode = root.path("choices").path(0).path("delta").path("content");
        if (!deltaNode.isMissingNode() && !deltaNode.isNull()) {
            return normalizeContent(deltaNode);
        }

        JsonNode messageNode = root.path("choices").path(0).path("message").path("content");
        if (!messageNode.isMissingNode() && !messageNode.isNull()) {
            return normalizeContent(messageNode);
        }

        JsonNode contentNode = root.path("choices").path(0).path("content");
        if (!contentNode.isMissingNode() && !contentNode.isNull()) {
            return normalizeContent(contentNode);
        }

        return "";
    }

    private String normalizeContent(JsonNode contentNode) {
        if (contentNode.isTextual()) {
            return contentNode.asText();
        }
        if (contentNode.isArray()) {
            StringBuilder builder = new StringBuilder();
            for (JsonNode item : contentNode) {
                if (item == null || item.isNull()) {
                    continue;
                }
                if (item.isTextual()) {
                    appendSegment(builder, item.asText());
                    continue;
                }
                JsonNode textNode = item.path("text");
                if (textNode.isTextual()) {
                    appendSegment(builder, textNode.asText());
                }
            }
            return builder.toString();
        }
        return contentNode.toString();
    }

    private void appendSegment(StringBuilder builder, String value) {
        if (!StringUtils.hasText(value)) {
            return;
        }
        builder.append(value);
    }

    private String readErrorMessage(String body, String fallback) {
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode messageNode = root.path("error").path("message");
            if (messageNode.isTextual()) {
                return messageNode.asText();
            }
            messageNode = root.path("message");
            if (messageNode.isTextual()) {
                return messageNode.asText();
            }
        } catch (Exception ignored) {
            // Fall back to the plain message when the body is not valid JSON.
        }
        return fallback;
    }
}
