package com.opsany.replica.security;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opsany.replica.config.AppProperties;
import com.opsany.replica.domain.AppUser;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SessionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SessionService.class);
    private static final String SESSION_KEY_PREFIX = "opsany:session:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final AppProperties appProperties;

    public String createSession(AppUser user) {
        String token = UUID.randomUUID().toString().replace("-", "");
        SessionUser sessionUser = new SessionUser(user.getId(), user.getUsername(), user.getDisplayName());
        try {
            redisTemplate.opsForValue().set(
                SESSION_KEY_PREFIX + token,
                objectMapper.writeValueAsString(sessionUser),
                Duration.ofHours(appProperties.getSession().getTtlHours())
            );
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize session user", exception);
        }
        return token;
    }

    public Optional<SessionUser> resolve(String token) {
        if (!StringUtils.hasText(token)) {
            return Optional.empty();
        }
        try {
            String payload = redisTemplate.opsForValue().get(SESSION_KEY_PREFIX + token);
            if (!StringUtils.hasText(payload)) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(payload, SessionUser.class));
        } catch (Exception exception) {
            LOGGER.warn("Failed to resolve session from Redis: {}", exception.getMessage());
            return Optional.empty();
        }
    }

    public void destroy(String token) {
        if (!StringUtils.hasText(token)) {
            return;
        }
        try {
            redisTemplate.delete(SESSION_KEY_PREFIX + token);
        } catch (Exception exception) {
            LOGGER.warn("Failed to delete session from Redis: {}", exception.getMessage());
        }
    }
}
