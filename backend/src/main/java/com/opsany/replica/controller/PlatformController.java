package com.opsany.replica.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.opsany.replica.security.AuthInterceptor;
import com.opsany.replica.security.SessionUser;
import com.opsany.replica.service.PlatformBootstrapService;

import javax.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/workbench")
@RequiredArgsConstructor
public class PlatformController {

    private final PlatformBootstrapService platformBootstrapService;

    @GetMapping("/bootstrap")
    public ObjectNode bootstrap(HttpServletRequest request, @RequestParam(value = "path", required = false) String path) {
        SessionUser sessionUser = (SessionUser) request.getAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE);
        return platformBootstrapService.getBootstrap(sessionUser, path);
    }
}
