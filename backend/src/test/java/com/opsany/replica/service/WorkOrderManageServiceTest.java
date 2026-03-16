package com.opsany.replica.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import com.opsany.replica.domain.WorkOrder;
import com.opsany.replica.domain.WorkOrderExt;
import com.opsany.replica.domain.WorkOrderStatusConfig;
import com.opsany.replica.dto.CreateWorkOrderRequest;
import com.opsany.replica.dto.WorkOrderDetailResponse;
import com.opsany.replica.repository.WorkOrderAttachmentRepository;
import com.opsany.replica.repository.WorkOrderExtRepository;
import com.opsany.replica.repository.WorkOrderHistoryRepository;
import com.opsany.replica.repository.WorkOrderMergeRepository;
import com.opsany.replica.repository.WorkOrderProcessLogRepository;
import com.opsany.replica.repository.WorkOrderReminderConfigRepository;
import com.opsany.replica.repository.WorkOrderReminderRepository;
import com.opsany.replica.repository.WorkOrderRepository;
import com.opsany.replica.repository.WorkOrderResourceRepository;
import com.opsany.replica.repository.WorkOrderSatisfactionConfigRepository;
import com.opsany.replica.repository.WorkOrderSatisfactionRepository;
import com.opsany.replica.repository.WorkOrderSourceRepository;
import com.opsany.replica.repository.WorkOrderSplitRepository;
import com.opsany.replica.security.SessionUser;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@ExtendWith(MockitoExtension.class)
class WorkOrderManageServiceTest {

    @Mock
    private WorkOrderRepository workOrderRepository;

    @Mock
    private WorkOrderHistoryRepository workOrderHistoryRepository;

    @Mock
    private WorkOrderExtRepository workOrderExtRepository;

    @Mock
    private WorkOrderSourceRepository workOrderSourceRepository;

    @Mock
    private WorkOrderResourceRepository workOrderResourceRepository;

    @Mock
    private WorkOrderProcessLogRepository processLogRepository;

    @Mock
    private WorkOrderSatisfactionRepository satisfactionRepository;

    @Mock
    private WorkOrderSatisfactionConfigRepository satisfactionConfigRepository;

    @Mock
    private WorkOrderReminderRepository reminderRepository;

    @Mock
    private WorkOrderReminderConfigRepository reminderConfigRepository;

    @Mock
    private WorkOrderMergeRepository mergeRepository;

    @Mock
    private WorkOrderSplitRepository splitRepository;

    @Mock
    private WorkOrderAttachmentRepository attachmentRepository;

    @Mock
    private WorkOrderNoRuleService workOrderNoRuleService;

    @Mock
    private WorkOrderStatusService workOrderStatusService;

    @InjectMocks
    private WorkOrderManageService workOrderManageService;

    private SessionUser testUser;
    private WorkOrder testOrder;
    private WorkOrderExt testExt;
    private WorkOrderStatusConfig testStatus;

    @BeforeEach
    void setUp() {
        testUser = new SessionUser(1L, "testuser", "测试用户", Arrays.asList("PLATFORM_ADMIN"));

        testStatus = WorkOrderStatusConfig.builder()
            .id(1L)
            .statusCode("PENDING")
            .statusName("待受理")
            .statusType("OPEN")
            .isInitial(true)
            .enabled(true)
            .build();

        testOrder = WorkOrder.builder()
            .id(1L)
            .orderNo("WO20260316000001")
            .title("测试工单")
            .type("请求管理")
            .creatorUsername("testuser")
            .creatorDisplayName("测试用户")
            .status("PENDING")
            .priority("中")
            .serviceName("测试服务")
            .description("测试描述")
            .progress("待受理")
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

        testExt = WorkOrderExt.builder()
            .id(1L)
            .orderNo("WO20260316000001")
            .source("MANUAL")
            .isRequesterConfirmed(false)
            .isMerged(false)
            .isSplit(false)
            .reminderCount(0)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
    }

    @Test
    void testCreateOrder_success() {
        CreateWorkOrderRequest request = CreateWorkOrderRequest.builder()
            .title("新工单")
            .type("请求管理")
            .priority("高")
            .serviceName("测试服务")
            .description("新工单描述")
            .build();

        when(workOrderNoRuleService.generateOrderNo()).thenReturn("WO20260316000001");
        when(workOrderStatusService.getInitialStatus()).thenReturn(testStatus);
        when(workOrderRepository.insert(any(WorkOrder.class))).thenReturn(1);
        when(workOrderExtRepository.insert(any(WorkOrderExt.class))).thenReturn(1);
        when(workOrderHistoryRepository.insert(any())).thenReturn(1);

        WorkOrder result = workOrderManageService.createOrder(request, testUser);

        assertNotNull(result);
        assertEquals("新工单", result.getTitle());
        assertEquals("testuser", result.getCreatorUsername());
    }

    @Test
    void testGetOrderDetail_success() {
        when(workOrderRepository.findByOrderNo("WO20260316000001")).thenReturn(testOrder);
        when(workOrderExtRepository.findByOrderNo("WO20260316000001")).thenReturn(testExt);
        when(workOrderHistoryRepository.findByOrderNo("WO20260316000001")).thenReturn(Arrays.asList());
        when(attachmentRepository.findByOrderNo("WO20260316000001")).thenReturn(Arrays.asList());

        WorkOrderDetailResponse result = workOrderManageService.getOrderDetail("WO20260316000001");

        assertNotNull(result);
        assertEquals("WO20260316000001", result.getOrderNo());
        assertEquals("测试工单", result.getTitle());
    }

    @Test
    void testGetOrderDetail_notFound() {
        when(workOrderRepository.findByOrderNo("NOTEXIST")).thenReturn(null);

        assertThrows(ResponseStatusException.class, () -> {
            workOrderManageService.getOrderDetail("NOTEXIST");
        });
    }

    @Test
    void testDeleteOrder_withAdmin() {
        testUser.setRoleCodes(Arrays.asList("PLATFORM_ADMIN"));
        when(workOrderRepository.findByOrderNo("WO20260316000001")).thenReturn(testOrder);
        when(workOrderHistoryRepository.insert(any())).thenReturn(1);

        workOrderManageService.deleteOrder("WO20260316000001", testUser);
    }

    @Test
    void testDeleteOrder_withoutAdminPermission() {
        testUser.setRoleCodes(Arrays.asList("USER"));
        when(workOrderRepository.findByOrderNo("WO20260316000001")).thenReturn(testOrder);

        assertThrows(ResponseStatusException.class, () -> {
            workOrderManageService.deleteOrder("WO20260316000001", testUser);
        });
    }
}
