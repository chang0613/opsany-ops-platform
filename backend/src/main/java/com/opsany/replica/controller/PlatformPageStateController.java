package com.opsany.replica.controller;

import javax.servlet.http.HttpServletRequest;

import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.opsany.replica.dto.SavePlatformPageStateRequest;
import com.opsany.replica.security.AuthInterceptor;
import com.opsany.replica.security.SessionUser;
import com.opsany.replica.service.PlatformBootstrapService;
import com.opsany.replica.service.PlatformPageStateService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/workbench/platform-pages")
@RequiredArgsConstructor
public class PlatformPageStateController {

    private final PlatformPageStateService platformPageStateService;
    private final PlatformBootstrapService platformBootstrapService;

    @PutMapping("/state")
    public void saveState(@RequestBody SavePlatformPageStateRequest request, HttpServletRequest servletRequest) {
        SessionUser sessionUser = (SessionUser) servletRequest.getAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE);
        platformPageStateService.saveState(request, sessionUser.getUsername());
        platformBootstrapService.evictAllBootstrapCaches();
    }
}
