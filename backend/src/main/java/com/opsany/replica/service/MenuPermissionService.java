package com.opsany.replica.service;

import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Service;

import com.opsany.replica.domain.MenuPermission;
import com.opsany.replica.repository.MenuRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MenuPermissionService {

    private final MenuRepository menuRepository;

    public List<MenuPermission> findMenusByUserId(Long userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        return menuRepository.findMenusByUserId(userId);
    }

    public String resolveLandingRoute(Long userId) {
        String route = menuRepository.findFirstRouteByUserId(userId);
        return route == null || route.trim().isEmpty() ? "/" : route;
    }
}
