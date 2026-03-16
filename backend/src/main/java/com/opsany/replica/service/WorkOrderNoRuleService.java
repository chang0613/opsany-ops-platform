package com.opsany.replica.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.opsany.replica.domain.WorkOrderNoRule;
import com.opsany.replica.repository.WorkOrderNoRuleRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkOrderNoRuleService {

    private final WorkOrderNoRuleRepository workOrderNoRuleRepository;

    private static final String DEFAULT_RULE_NAME = "default";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd", Locale.CHINA);

    @Transactional
    public String generateOrderNo() {
        WorkOrderNoRule rule = workOrderNoRuleRepository.findByEnabledTrue();
        if (rule == null) {
            rule = getDefaultRule();
        }

        String prefix = rule.getPrefix();
        String dateStr = LocalDateTime.now().format(DATE_FORMATTER);

        int sequence = rule.getSequenceCurrent();
        String sequenceStr = String.format("%0" + rule.getSequenceLength() + "d", sequence);

        String orderNo = prefix + dateStr + sequenceStr;

        workOrderNoRuleRepository.updateSequenceCurrent(rule.getId(), sequence + 1);

        return orderNo;
    }

    private WorkOrderNoRule getDefaultRule() {
        WorkOrderNoRule rule = WorkOrderNoRule.builder()
            .ruleName(DEFAULT_RULE_NAME)
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
        workOrderNoRuleRepository.insert(rule);
        return rule;
    }

    public WorkOrderNoRule getRuleByName(String ruleName) {
        return workOrderNoRuleRepository.findByRuleName(ruleName);
    }

    public WorkOrderNoRule getEnabledRule() {
        return workOrderNoRuleRepository.findByEnabledTrue();
    }

    public java.util.List<WorkOrderNoRule> getAllRules() {
        return workOrderNoRuleRepository.findAll();
    }

    @Transactional
    public WorkOrderNoRule createRule(WorkOrderNoRule rule) {
        rule.setCreatedAt(LocalDateTime.now());
        rule.setUpdatedAt(LocalDateTime.now());
        workOrderNoRuleRepository.insert(rule);
        return rule;
    }

    @Transactional
    public WorkOrderNoRule updateRule(WorkOrderNoRule rule) {
        rule.setUpdatedAt(LocalDateTime.now());
        workOrderNoRuleRepository.update(rule);
        return rule;
    }

    @Transactional
    public void deleteRule(Long id) {
        workOrderNoRuleRepository.deleteById(id);
    }
}
