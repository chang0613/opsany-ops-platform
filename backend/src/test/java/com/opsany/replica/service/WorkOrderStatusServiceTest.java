package com.opsany.replica.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.opsany.replica.domain.WorkOrderStatusConfig;
import com.opsany.replica.domain.WorkOrderTransitionPermission;
import com.opsany.replica.repository.WorkOrderStatusConfigRepository;
import com.opsany.replica.repository.WorkOrderTransitionPermissionRepository;
import com.opsany.replica.security.SessionUser;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@ExtendWith(MockitoExtension.class)
class WorkOrderStatusServiceTest {

    @Mock
    private WorkOrderStatusConfigRepository statusConfigRepository;

    @Mock
    private WorkOrderTransitionPermissionRepository transitionPermissionRepository;

    @InjectMocks
    private WorkOrderStatusService workOrderStatusService;

    private WorkOrderStatusConfig testStatus;
    private WorkOrderTransitionPermission testPermission;

    @BeforeEach
    void setUp() {
        testStatus = WorkOrderStatusConfig.builder()
            .id(1L)
            .statusCode("PENDING")
            .statusName("待受理")
            .statusType("OPEN")
            .color("#FAAD14")
            .sortOrder(1)
            .isInitial(true)
            .isFinal(false)
            .enabled(true)
            .description("待受理状态")
            .build();

        testPermission = WorkOrderTransitionPermission.builder()
            .id(1L)
            .fromStatus("PENDING")
            .toStatus("PROCESSING")
            .roleCode("PLATFORM_ADMIN")
            .allowCreator(false)
            .allowHandler(true)
            .allowAdmin(true)
            .isAutoTransition(false)
            .build();
    }

    @Test
    void testGetAllStatusConfigs() {
        when(statusConfigRepository.findAll()).thenReturn(Arrays.asList(testStatus));

        List<WorkOrderStatusConfig> result = workOrderStatusService.getAllStatusConfigs();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("PENDING", result.get(0).getStatusCode());
    }

    @Test
    void testGetEnabledStatusConfigs() {
        when(statusConfigRepository.findEnabled()).thenReturn(Arrays.asList(testStatus));

        List<WorkOrderStatusConfig> result = workOrderStatusService.getEnabledStatusConfigs();

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void testGetStatusConfigByCode() {
        when(statusConfigRepository.findByStatusCode("PENDING")).thenReturn(testStatus);

        WorkOrderStatusConfig result = workOrderStatusService.getStatusConfigByCode("PENDING");

        assertNotNull(result);
        assertEquals("待受理", result.getStatusName());
    }

    @Test
    void testGetInitialStatus() {
        when(statusConfigRepository.findInitialStatus()).thenReturn(testStatus);

        WorkOrderStatusConfig result = workOrderStatusService.getInitialStatus();

        assertNotNull(result);
        assertEquals(true, result.getIsInitial());
    }

    @Test
    void testCanTransition_withAdmin() {
        SessionUser adminUser = new SessionUser(1L, "admin", "管理员", Arrays.asList("PLATFORM_ADMIN"));

        when(transitionPermissionRepository.findByFromStatus("PENDING"))
            .thenReturn(Arrays.asList(testPermission));

        boolean result = workOrderStatusService.canTransition("PENDING", "PROCESSING", adminUser);

        assertEquals(true, result);
    }
}
