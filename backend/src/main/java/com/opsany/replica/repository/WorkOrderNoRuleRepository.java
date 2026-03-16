package com.opsany.replica.repository;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.opsany.replica.domain.WorkOrderNoRule;

@Mapper
public interface WorkOrderNoRuleRepository {

    int insert(WorkOrderNoRule rule);

    int update(WorkOrderNoRule rule);

    int deleteById(@Param("id") Long id);

    WorkOrderNoRule findById(@Param("id") Long id);

    WorkOrderNoRule findByRuleName(@Param("ruleName") String ruleName);

    WorkOrderNoRule findByEnabledTrue();

    List<WorkOrderNoRule> findAll();

    int updateSequenceCurrent(@Param("id") Long id, @Param("sequenceCurrent") Integer sequenceCurrent);

    long count();
}
