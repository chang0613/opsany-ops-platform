package com.opsany.replica.repository;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.opsany.replica.domain.WorkOrderSatisfactionConfig;

@Mapper
public interface WorkOrderSatisfactionConfigRepository {

    int insert(WorkOrderSatisfactionConfig config);

    int update(WorkOrderSatisfactionConfig config);

    int deleteById(@Param("id") Long id);

    WorkOrderSatisfactionConfig findById(@Param("id") Long id);

    WorkOrderSatisfactionConfig findByScore(@Param("score") Integer score);

    List<WorkOrderSatisfactionConfig> findAll();

    List<WorkOrderSatisfactionConfig> findEnabled();

    long count();
}
