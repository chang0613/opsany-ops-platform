package com.opsany.replica.security;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opsany.replica.config.AppProperties;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SessionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SessionService.class);
    private static final String SESSION_KEY_PREFIX = "opsany:session:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final AppProperties appProperties;
    private final Map<String, InMemorySession> inMemorySessions = new ConcurrentHashMap<String, InMemorySession>();

    public String createSession(SessionUser sessionUser) {
        String token = UUID.randomUUID().toString().replace("-", "");
        String payload = serialize(sessionUser);

        if (shouldUseRedis()) {
            try {
                redisTemplate.opsForValue().set(
                    SESSION_KEY_PREFIX + token,
                    payload,
                    Duration.ofHours(appProperties.getSession().getTtlHours())
                );
                return token;
            } catch (Exception exception) {
                LOGGER.warn("Falling back to in-memory session storage: {}", exception.getMessage());
            }
        }

        inMemorySessions.put(token, new InMemorySession(
            sessionUser,
            LocalDateTime.now().plusHours(appProperties.getSession().getTtlHours())
        ));
        return token;
    }

    public Optional<SessionUser> resolve(String token) {
        if (!StringUtils.hasText(token)) {
            return Optional.empty();
        }

        if (shouldUseRedis()) {
            try {
                String payload = redisTemplate.opsForValue().get(SESSION_KEY_PREFIX + token);
                if (!StringUtils.hasText(payload)) {
                    return resolveInMemory(token);
                }
                return Optional.of(objectMapper.readValue(payload, SessionUser.class));
            } catch (Exception exception) {
                LOGGER.warn("Failed to resolve session from Redis, using in-memory fallback: {}", exception.getMessage());
            }
        }

        return resolveInMemory(token);
    }

    public void destroy(String token) {
        if (!StringUtils.hasText(token)) {
            return;
        }

        if (shouldUseRedis()) {
            try {
                redisTemplate.delete(SESSION_KEY_PREFIX + token);
            } catch (Exception exception) {
                LOGGER.warn("Failed to delete session from Redis: {}", exception.getMessage());
            }
        }

        inMemorySessions.remove(token);
    }

    private Optional<SessionUser> resolveInMemory(String token) {
        InMemorySession session = inMemorySessions.get(token);
        if (session == null) {
            return Optional.empty();
        }
        if (session.getExpiresAt().isBefore(LocalDateTime.now())) {
            inMemorySessions.remove(token);
            return Optional.empty();
        }
        return Optional.of(session.getUser());
    }

    private boolean shouldUseRedis() {
        return "redis".equalsIgnoreCase(appProperties.getSession().getStore());
    }

    private String serialize(SessionUser sessionUser) {
        try {
            return objectMapper.writeValueAsString(sessionUser);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize session user", exception);
        }
    }

    @lombok.Getter
    @AllArgsConstructor
    private static class InMemorySession {
        private final SessionUser user;
        private final LocalDateTime expiresAt;
    }
}
