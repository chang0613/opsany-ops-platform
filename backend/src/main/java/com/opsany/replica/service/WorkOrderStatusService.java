package com.opsany.replica.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.opsany.replica.domain.WorkOrderStatusConfig;
import com.opsany.replica.domain.WorkOrderTransitionPermission;
import com.opsany.replica.repository.WorkOrderStatusConfigRepository;
import com.opsany.replica.repository.WorkOrderTransitionPermissionRepository;
import com.opsany.replica.security.SessionUser;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkOrderStatusService {

    private final WorkOrderStatusConfigRepository statusConfigRepository;
    private final WorkOrderTransitionPermissionRepository transitionPermissionRepository;

    public List<WorkOrderStatusConfig> getAllStatusConfigs() {
        return statusConfigRepository.findAll();
    }

    public List<WorkOrderStatusConfig> getEnabledStatusConfigs() {
        return statusConfigRepository.findEnabled();
    }

    public WorkOrderStatusConfig getStatusConfigByCode(String statusCode) {
        return statusConfigRepository.findByStatusCode(statusCode);
    }

    public WorkOrderStatusConfig getInitialStatus() {
        WorkOrderStatusConfig status = statusConfigRepository.findInitialStatus();
        if (status == null) {
            status = statusConfigRepository.findByStatusCode("PENDING");
        }
        return status;
    }

    @Transactional
    public WorkOrderStatusConfig createStatusConfig(WorkOrderStatusConfig config) {
        config.setCreatedAt(LocalDateTime.now());
        config.setUpdatedAt(LocalDateTime.now());
        statusConfigRepository.insert(config);
        return config;
    }

    @Transactional
    public WorkOrderStatusConfig updateStatusConfig(WorkOrderStatusConfig config) {
        config.setUpdatedAt(LocalDateTime.now());
        statusConfigRepository.update(config);
        return config;
    }

    @Transactional
    public void deleteStatusConfig(Long id) {
        statusConfigRepository.deleteById(id);
    }

    public List<WorkOrderTransitionPermission> getTransitionPermissions() {
        return transitionPermissionRepository.findAll();
    }

    public List<WorkOrderTransitionPermission> getPermissionsByFromStatus(String fromStatus) {
        return transitionPermissionRepository.findByFromStatus(fromStatus);
    }

    public boolean canTransition(String fromStatus, String toStatus, SessionUser sessionUser) {
        List<WorkOrderTransitionPermission> permissions = transitionPermissionRepository.findByFromStatus(fromStatus);

        for (WorkOrderTransitionPermission permission : permissions) {
            if (permission.getToStatus().equals(toStatus)) {
                boolean isAdmin = isAdmin(sessionUser);
                boolean isCreator = sessionUser.getRoleCodes() != null && sessionUser.getRoleCodes().contains("CREATOR");
                boolean isHandler = sessionUser.getRoleCodes() != null && sessionUser.getRoleCodes().contains("HANDLER");

                if (isAdmin && permission.getAllowAdmin()) {
                    return true;
                }
                if (isCreator && permission.getAllowCreator()) {
                    return true;
                }
                if (isHandler && permission.getAllowHandler()) {
                    return true;
                }
            }
        }

        return false;
    }

    @Transactional
    public WorkOrderTransitionPermission createTransitionPermission(WorkOrderTransitionPermission permission) {
        permission.setCreatedAt(LocalDateTime.now());
        transitionPermissionRepository.insert(permission);
        return permission;
    }

    @Transactional
    public void deleteTransitionPermission(Long id) {
        transitionPermissionRepository.deleteById(id);
    }

    private boolean isAdmin(SessionUser sessionUser) {
        return sessionUser.getRoleCodes() != null && sessionUser.getRoleCodes().contains("PLATFORM_ADMIN");
    }
}
