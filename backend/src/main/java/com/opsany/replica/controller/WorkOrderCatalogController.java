package com.opsany.replica.controller;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.opsany.replica.domain.WorkOrderCatalog;
import com.opsany.replica.dto.SaveWorkOrderCatalogRequest;
import com.opsany.replica.security.AuthInterceptor;
import com.opsany.replica.security.SessionUser;
import com.opsany.replica.service.PlatformBootstrapService;
import com.opsany.replica.service.WorkOrderCatalogService;

import javax.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/workbench/catalogs")
@RequiredArgsConstructor
public class WorkOrderCatalogController {

    private final WorkOrderCatalogService workOrderCatalogService;
    private final PlatformBootstrapService platformBootstrapService;

    @GetMapping
    public List<WorkOrderCatalog> list() {
        return workOrderCatalogService.listAll();
    }

    @PostMapping
    public WorkOrderCatalog save(@RequestBody SaveWorkOrderCatalogRequest request, HttpServletRequest servletRequest) {
        SessionUser sessionUser = (SessionUser) servletRequest.getAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE);
        WorkOrderCatalog catalog = workOrderCatalogService.saveCatalog(
            request,
            sessionUser.getUsername(),
            sessionUser.getDisplayName()
        );
        platformBootstrapService.evictAllBootstrapCaches();
        return catalog;
    }

    @DeleteMapping("/{catalogCode}")
    public void delete(@PathVariable String catalogCode) {
        workOrderCatalogService.deleteByCatalogCode(catalogCode);
        platformBootstrapService.evictAllBootstrapCaches();
    }
}
