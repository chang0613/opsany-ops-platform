package com.opsany.replica.service;

import java.io.IOException;
import java.io.InputStream;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import javax.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PlatformTemplateService {

    private final ObjectMapper objectMapper;

    private ObjectNode template;

    @PostConstruct
    void loadTemplate() throws IOException {
        ClassPathResource resource = new ClassPathResource("seed/platform.json");
        try (InputStream inputStream = resource.getInputStream()) {
            template = (ObjectNode) objectMapper.readTree(inputStream);
        }
    }

    public ObjectNode copyTemplate() {
        return template.deepCopy();
    }
}
