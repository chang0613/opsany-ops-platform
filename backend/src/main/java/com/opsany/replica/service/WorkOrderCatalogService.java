package com.opsany.replica.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.opsany.replica.domain.AppUser;
import com.opsany.replica.domain.WorkOrderCatalog;
import com.opsany.replica.dto.SaveWorkOrderCatalogRequest;
import com.opsany.replica.repository.AppUserRepository;
import com.opsany.replica.repository.WorkOrderCatalogRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WorkOrderCatalogService {

    private final WorkOrderCatalogRepository workOrderCatalogRepository;
    private final AppUserRepository appUserRepository;

    public List<WorkOrderCatalog> listAll() {
        return workOrderCatalogRepository.findAll();
    }

    public List<WorkOrderCatalog> listOnlineCatalogs() {
        return workOrderCatalogRepository.findOnlineCatalogs();
    }

    public WorkOrderCatalog findByCatalogCode(String catalogCode) {
        return workOrderCatalogRepository.findByCatalogCode(catalogCode);
    }

    public WorkOrderCatalog findByName(String name) {
        return workOrderCatalogRepository.findByName(name);
    }

    public WorkOrderCatalog saveCatalog(SaveWorkOrderCatalogRequest request, String operatorUsername, String operatorDisplayName) {
        String catalogCode = StringUtils.hasText(request.getCatalogCode()) ? request.getCatalogCode() : nextCatalogCode();
        WorkOrderCatalog existing = workOrderCatalogRepository.findByCatalogCode(catalogCode);
        AppUser owner = resolveOwner(request.getOwnerUsername(), operatorUsername);
        LocalDateTime now = LocalDateTime.now();

        WorkOrderCatalog catalog = WorkOrderCatalog.builder()
            .catalogCode(catalogCode)
            .name(defaultIfBlank(request.getName(), "未命名工单目录"))
            .category(defaultIfBlank(request.getCategory(), "默认分组"))
            .type(defaultIfBlank(request.getType(), "请求管理"))
            .scope(defaultIfBlank(request.getScope(), "全部用户"))
            .online(request.getOnline() == null ? Boolean.TRUE : request.getOnline())
            .processCode(defaultIfBlank(request.getProcessCode(), WorkOrderProcessService.DEFAULT_PROCESS_CODE))
            .slaName(defaultIfBlank(request.getSlaName(), "-"))
            .ownerUsername(owner == null ? defaultIfBlank(operatorUsername, "admin") : owner.getUsername())
            .ownerDisplayName(owner == null ? defaultIfBlank(operatorDisplayName, "管理员") : owner.getDisplayName())
            .description(defaultIfBlank(request.getDescription(), "标准服务目录"))
            .sortNo(request.getSortNo() == null ? 0 : request.getSortNo())
            .createdAt(existing == null ? now : existing.getCreatedAt())
            .updatedAt(now)
            .build();

        if (existing == null) {
            workOrderCatalogRepository.insert(catalog);
        } else {
            workOrderCatalogRepository.update(catalog);
        }
        return workOrderCatalogRepository.findByCatalogCode(catalogCode);
    }

    public void deleteByCatalogCode(String catalogCode) {
        if (!StringUtils.hasText(catalogCode)) {
            return;
        }
        workOrderCatalogRepository.deleteByCatalogCode(catalogCode);
    }

    private AppUser resolveOwner(String requestOwnerUsername, String operatorUsername) {
        if (StringUtils.hasText(requestOwnerUsername)) {
            AppUser owner = appUserRepository.findByUsername(requestOwnerUsername);
            if (owner != null) {
                return owner;
            }
        }
        if (StringUtils.hasText(operatorUsername)) {
            return appUserRepository.findByUsername(operatorUsername);
        }
        return null;
    }

    private String nextCatalogCode() {
        return "CATALOG_" + System.currentTimeMillis() + ThreadLocalRandom.current().nextInt(10, 99);
    }

    private String defaultIfBlank(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }
}
