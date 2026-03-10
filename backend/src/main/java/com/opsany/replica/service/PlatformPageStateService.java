package com.opsany.replica.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.opsany.replica.domain.PlatformPageState;
import com.opsany.replica.dto.SavePlatformPageStateRequest;
import com.opsany.replica.repository.PlatformPageStateRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PlatformPageStateService {

    private final PlatformPageStateRepository platformPageStateRepository;
    private final ObjectMapper objectMapper;

    public void saveState(SavePlatformPageStateRequest request, String username) {
        String platformKey = normalizePlatformKey(request.getPlatformKey());
        String pageKey = normalizePageKey(request.getPageKey());
        JsonNode tableStates = request.getTableStates();
        PlatformPageState existing = platformPageStateRepository.findByPlatformKeyAndPageKey(platformKey, pageKey);
        PlatformPageState state = PlatformPageState.builder()
            .id(existing == null ? null : existing.getId())
            .platformKey(platformKey)
            .pageKey(pageKey)
            .stateJson(writeTableStates(tableStates))
            .updatedBy(defaultIfBlank(username, "system"))
            .updatedAt(LocalDateTime.now())
            .build();

        if (existing == null) {
            platformPageStateRepository.insert(state);
            return;
        }
        platformPageStateRepository.update(state);
    }

    public void applyState(ObjectNode root, String platformKey) {
        ObjectNode pages = root.with("pages");
        ensureTableKeys(pages);

        List<PlatformPageState> states = platformPageStateRepository.findByPlatformKey(normalizePlatformKey(platformKey));
        for (PlatformPageState state : states) {
            JsonNode pageNode = pages.get(normalizePageKey(state.getPageKey()));
            if (!(pageNode instanceof ObjectNode)) {
                continue;
            }
            applyTableState((ObjectNode) pageNode, readTableState(state.getStateJson()));
        }
    }

    public void ensureTableKeys(ObjectNode pages) {
        java.util.Iterator<Map.Entry<String, JsonNode>> iterator = pages.fields();
        while (iterator.hasNext()) {
            Map.Entry<String, JsonNode> entry = iterator.next();
            JsonNode pageNode = entry.getValue();
            if (!(pageNode instanceof ObjectNode)) {
                continue;
            }
            ArrayNode tables = ((ObjectNode) pageNode).withArray("tables");
            for (int index = 0; index < tables.size(); index++) {
                JsonNode tableNode = tables.get(index);
                if (!(tableNode instanceof ObjectNode)) {
                    continue;
                }
                ObjectNode objectNode = (ObjectNode) tableNode;
                if (!StringUtils.hasText(objectNode.path("key").asText())) {
                    objectNode.put("key", "table_" + (index + 1));
                }
            }
        }
    }

    private void applyTableState(ObjectNode pageNode, JsonNode tableStateNode) {
        if (tableStateNode == null || !tableStateNode.isArray()) {
            return;
        }

        Map<String, JsonNode> rowsByTableKey = new LinkedHashMap<String, JsonNode>();
        for (JsonNode item : tableStateNode) {
            if (item == null || !item.isObject()) {
                continue;
            }
            String tableKey = item.path("tableKey").asText();
            JsonNode rows = item.get("rows");
            if (StringUtils.hasText(tableKey) && rows != null && rows.isArray()) {
                rowsByTableKey.put(tableKey, rows);
            }
        }

        ArrayNode tables = pageNode.withArray("tables");
        for (int index = 0; index < tables.size(); index++) {
            JsonNode tableNode = tables.get(index);
            if (!(tableNode instanceof ObjectNode)) {
                continue;
            }
            ObjectNode objectNode = (ObjectNode) tableNode;
            String tableKey = objectNode.path("key").asText("table_" + (index + 1));
            JsonNode rows = rowsByTableKey.get(tableKey);
            if (rows != null && rows.isArray()) {
                objectNode.set("rows", rows.deepCopy());
            }
        }
    }

    private JsonNode readTableState(String stateJson) {
        if (!StringUtils.hasText(stateJson)) {
            return objectMapper.createArrayNode();
        }
        try {
            return objectMapper.readTree(stateJson);
        } catch (IOException exception) {
            return objectMapper.createArrayNode();
        }
    }

    private String writeTableStates(JsonNode tableStates) {
        try {
            return objectMapper.writeValueAsString(tableStates == null ? objectMapper.createArrayNode() : tableStates);
        } catch (IOException exception) {
            return "[]";
        }
    }

    private String normalizePlatformKey(String platformKey) {
        return defaultIfBlank(platformKey, "workbench");
    }

    private String normalizePageKey(String pageKey) {
        return StringUtils.hasText(pageKey) ? pageKey.trim() : "/";
    }

    private String defaultIfBlank(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }
}
