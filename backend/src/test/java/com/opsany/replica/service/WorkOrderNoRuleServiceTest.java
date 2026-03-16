package com.opsany.replica.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.opsany.replica.domain.WorkOrderNoRule;
import com.opsany.replica.repository.WorkOrderNoRuleRepository;
import com.opsany.replica.security.SessionUser;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@ExtendWith(MockitoExtension.class)
class WorkOrderNoRuleServiceTest {

    @Mock
    private WorkOrderNoRuleRepository workOrderNoRuleRepository;

    @InjectMocks
    private WorkOrderNoRuleService workOrderNoRuleService;

    private WorkOrderNoRule testRule;

    @BeforeEach
    void setUp() {
        testRule = WorkOrderNoRule.builder()
            .id(1L)
            .ruleName("default")
            .prefix("WO")
            .sequenceStart(1)
            .sequenceCurrent(1)
            .sequenceLength(6)
            .dateFormat("yyyyMMdd")
            .enabled(true)
            .description("默认工单编号规则")
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
    }

    @Test
    void testGenerateOrderNo_withExistingRule() {
        when(workOrderNoRuleRepository.findByEnabledTrue()).thenReturn(testRule);
        when(workOrderNoRuleRepository.updateSequenceCurrent(any(), any())).thenReturn(1);

        String orderNo = workOrderNoRuleService.generateOrderNo();

        assertNotNull(orderNo);
        assertEquals(12, orderNo.length());
        assertEquals("WO", orderNo.substring(0, 2));
    }

    @Test
    void testGenerateOrderNo_withDefaultRule() {
        when(workOrderNoRuleRepository.findByEnabledTrue()).thenReturn(null);
        when(workOrderNoRuleRepository.insert(any(WorkOrderNoRule.class))).thenReturn(1);

        String orderNo = workOrderNoRuleService.generateOrderNo();

        assertNotNull(orderNo);
        assertEquals("WO", orderNo.substring(0, 2));
    }

    @Test
    void testGetRuleByName() {
        when(workOrderNoRuleRepository.findByRuleName("default")).thenReturn(testRule);

        WorkOrderNoRule result = workOrderNoRuleService.getRuleByName("default");

        assertNotNull(result);
        assertEquals("default", result.getRuleName());
        assertEquals("WO", result.getPrefix());
    }

    @Test
    void testGetAllRules() {
        when(workOrderNoRuleRepository.findAll()).thenReturn(java.util.Collections.singletonList(testRule));

        java.util.List<WorkOrderNoRule> rules = workOrderNoRuleService.getAllRules();

        assertNotNull(rules);
        assertEquals(1, rules.size());
    }
}
