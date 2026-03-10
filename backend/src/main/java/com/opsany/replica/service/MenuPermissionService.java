package com.opsany.replica.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.opsany.replica.domain.AppRole;
import com.opsany.replica.domain.MenuPermission;
import com.opsany.replica.dto.MenuConfigItem;
import com.opsany.replica.dto.MenuConfigResponse;
import com.opsany.replica.dto.MenuConfigSaveRequest;
import com.opsany.replica.repository.MenuRepository;
import com.opsany.replica.repository.RoleRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MenuPermissionService {

    private final MenuRepository menuRepository;
    private final RoleRepository roleRepository;

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

    public MenuConfigResponse getMenuConfig() {
        List<AppRole> roles = roleRepository.findAllRoles();
        List<MenuConfigItem> items = new ArrayList<MenuConfigItem>();
        for (MenuPermission menu : menuRepository.findAllMenus()) {
            items.add(toConfigItem(menu));
        }
        return new MenuConfigResponse(roles, items);
    }

    public MenuConfigItem saveMenu(MenuConfigSaveRequest request) {
        String menuCode = StringUtils.hasText(request.getMenuCode()) ? request.getMenuCode() : nextMenuCode(request.getLabel());
        MenuPermission menu = MenuPermission.builder()
            .menuCode(menuCode)
            .groupName(defaultIfBlank(request.getGroupName(), "工作台"))
            .groupSortNo(request.getGroupSortNo() == null ? 1 : request.getGroupSortNo())
            .label(defaultIfBlank(request.getLabel(), "未命名菜单"))
            .route(defaultIfBlank(request.getRoute(), "/"))
            .icon(request.getIcon())
            .sortNo(request.getSortNo() == null ? 0 : request.getSortNo())
            .permissionCode(request.getPermissionCode())
            .visible(request.getVisible() == null ? Boolean.TRUE : request.getVisible())
            .build();

        if (menuRepository.countByMenuCode(menuCode) == 0) {
            menuRepository.insert(menu);
        } else {
            menuRepository.update(menu);
        }

        menuRepository.deleteRoleMenusByMenuCode(menuCode);
        if (request.getRoleCodes() != null) {
            for (String roleCode : request.getRoleCodes()) {
                menuRepository.grantRoleMenu(roleCode, menuCode);
            }
        }
        return toConfigItem(menu);
    }

    public void deleteMenu(String menuCode) {
        if (!StringUtils.hasText(menuCode)) {
            return;
        }
        menuRepository.deleteRoleMenusByMenuCode(menuCode);
        menuRepository.deleteMenuByCode(menuCode);
    }

    private MenuConfigItem toConfigItem(MenuPermission menu) {
        return new MenuConfigItem(
            menu.getId(),
            menu.getMenuCode(),
            menu.getGroupName(),
            menu.getGroupSortNo(),
            menu.getLabel(),
            menu.getRoute(),
            menu.getIcon(),
            menu.getSortNo(),
            menu.getPermissionCode(),
            menu.getVisible(),
            menuRepository.findRoleCodesByMenuCode(menu.getMenuCode())
        );
    }

    private String nextMenuCode(String label) {
        String normalized = label == null ? "MENU" : label.trim().replaceAll("[^A-Za-z0-9]+", "_").toUpperCase();
        if (!StringUtils.hasText(normalized)) {
            normalized = "MENU";
        }
        return normalized + "_" + System.currentTimeMillis();
    }

    private String defaultIfBlank(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }
}
