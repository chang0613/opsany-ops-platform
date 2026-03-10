package com.opsany.replica.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import com.opsany.replica.domain.MessageSubscription;
import com.opsany.replica.dto.MessageSubscriptionPayload;
import com.opsany.replica.dto.UserOption;
import com.opsany.replica.security.AuthInterceptor;
import com.opsany.replica.security.SessionUser;
import com.opsany.replica.repository.AppUserRepository;
import com.opsany.replica.service.MessageSubscriptionService;
import com.opsany.replica.service.PlatformBootstrapService;

import javax.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/workbench/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final AppUserRepository appUserRepository;
    private final MessageSubscriptionService messageSubscriptionService;
    private final PlatformBootstrapService platformBootstrapService;

    @GetMapping("/users")
    public List<UserOption> users() {
        return appUserRepository.findAllUserOptions();
    }

    @GetMapping
    public List<MessageSubscription> list(
        @RequestParam(required = false) String username,
        HttpServletRequest servletRequest
    ) {
        SessionUser sessionUser = (SessionUser) servletRequest.getAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE);
        String targetUsername = resolveTargetUsername(username, sessionUser);
        return messageSubscriptionService.listByUsername(targetUsername);
    }

    @PutMapping
    public void save(
        @RequestParam(required = false) String username,
        @RequestBody List<MessageSubscriptionPayload> payloads,
        HttpServletRequest servletRequest
    ) {
        SessionUser sessionUser = (SessionUser) servletRequest.getAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE);
        String targetUsername = resolveTargetUsername(username, sessionUser);
        messageSubscriptionService.saveSubscriptions(targetUsername, payloads);
        platformBootstrapService.evictBootstrapCacheByUsername(targetUsername);
    }

    private String resolveTargetUsername(String username, SessionUser sessionUser) {
        if (!StringUtils.hasText(username) || username.equals(sessionUser.getUsername())) {
            return sessionUser.getUsername();
        }
        if (sessionUser.getRoleCodes() != null && sessionUser.getRoleCodes().contains("PLATFORM_ADMIN")) {
            return username;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "当前用户不能管理其他人的订阅设置");
    }
}
