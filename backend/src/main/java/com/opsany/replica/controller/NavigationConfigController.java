package com.opsany.replica.controller;

import javax.servlet.http.HttpServletRequest;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.opsany.replica.dto.NavigationConfigResponse;
import com.opsany.replica.dto.NavigationFavoriteState;
import com.opsany.replica.dto.NavigationGroupDto;
import com.opsany.replica.dto.NavigationGroupSaveRequest;
import com.opsany.replica.dto.NavigationItemDto;
import com.opsany.replica.dto.NavigationItemSaveRequest;
import com.opsany.replica.security.AuthInterceptor;
import com.opsany.replica.security.SessionUser;
import com.opsany.replica.service.PlatformBootstrapService;
import com.opsany.replica.service.PlatformNavigationService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/workbench/navigation-config")
@RequiredArgsConstructor
public class NavigationConfigController {

    private final PlatformNavigationService platformNavigationService;
    private final PlatformBootstrapService platformBootstrapService;

    @GetMapping
    public NavigationConfigResponse getConfig(HttpServletRequest request) {
        SessionUser sessionUser = (SessionUser) request.getAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE);
        return platformNavigationService.getNavigationConfig(sessionUser.getUserId());
    }

    @PostMapping("/groups")
    public NavigationGroupDto saveGroup(@RequestBody NavigationGroupSaveRequest request) {
        NavigationGroupDto group = platformNavigationService.saveGroup(request);
        platformBootstrapService.evictAllBootstrapCaches();
        return group;
    }

    @PostMapping("/items")
    public NavigationItemDto saveItem(@RequestBody NavigationItemSaveRequest request, HttpServletRequest servletRequest) {
        SessionUser sessionUser = (SessionUser) servletRequest.getAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE);
        NavigationItemDto item = platformNavigationService.saveItem(request, sessionUser);
        platformBootstrapService.evictAllBootstrapCaches();
        return item;
    }

    @DeleteMapping("/items/{itemCode}")
    public void deleteItem(@PathVariable String itemCode) {
        platformNavigationService.deleteItem(itemCode);
        platformBootstrapService.evictAllBootstrapCaches();
    }

    @PostMapping("/favorites/{itemCode}")
    public NavigationFavoriteState toggleFavorite(@PathVariable String itemCode, HttpServletRequest request) {
        SessionUser sessionUser = (SessionUser) request.getAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE);
        NavigationFavoriteState state = platformNavigationService.toggleFavorite(sessionUser.getUserId(), itemCode);
        platformBootstrapService.evictBootstrapCache(sessionUser.getUserId());
        return state;
    }
}
