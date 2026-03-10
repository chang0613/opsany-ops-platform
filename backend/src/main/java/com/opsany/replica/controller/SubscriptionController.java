package com.opsany.replica.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.opsany.replica.domain.MessageSubscription;
import com.opsany.replica.dto.MessageSubscriptionPayload;
import com.opsany.replica.security.AuthInterceptor;
import com.opsany.replica.security.SessionUser;
import com.opsany.replica.service.MessageSubscriptionService;
import com.opsany.replica.service.PlatformBootstrapService;

import javax.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/workbench/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final MessageSubscriptionService messageSubscriptionService;
    private final PlatformBootstrapService platformBootstrapService;

    @GetMapping
    public List<MessageSubscription> list(HttpServletRequest servletRequest) {
        SessionUser sessionUser = (SessionUser) servletRequest.getAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE);
        return messageSubscriptionService.listByUsername(sessionUser.getUsername());
    }

    @PutMapping
    public void save(@RequestBody List<MessageSubscriptionPayload> payloads, HttpServletRequest servletRequest) {
        SessionUser sessionUser = (SessionUser) servletRequest.getAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE);
        messageSubscriptionService.saveSubscriptions(sessionUser.getUsername(), payloads);
        platformBootstrapService.evictBootstrapCache(sessionUser.getUserId());
    }
}
