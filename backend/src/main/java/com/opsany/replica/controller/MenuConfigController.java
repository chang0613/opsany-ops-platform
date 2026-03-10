package com.opsany.replica.controller;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.opsany.replica.dto.MenuConfigItem;
import com.opsany.replica.dto.MenuConfigResponse;
import com.opsany.replica.dto.MenuConfigSaveRequest;
import com.opsany.replica.service.MenuPermissionService;
import com.opsany.replica.service.PlatformBootstrapService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/workbench/menu-config")
@RequiredArgsConstructor
public class MenuConfigController {

    private final MenuPermissionService menuPermissionService;
    private final PlatformBootstrapService platformBootstrapService;

    @GetMapping
    public MenuConfigResponse getConfig() {
        return menuPermissionService.getMenuConfig();
    }

    @PostMapping("/menus")
    public MenuConfigItem saveMenu(@RequestBody MenuConfigSaveRequest request) {
        MenuConfigItem item = menuPermissionService.saveMenu(request);
        platformBootstrapService.evictAllBootstrapCaches();
        return item;
    }

    @DeleteMapping("/menus/{menuCode}")
    public void deleteMenu(@PathVariable String menuCode) {
        menuPermissionService.deleteMenu(menuCode);
        platformBootstrapService.evictAllBootstrapCaches();
    }
}
