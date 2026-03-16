package com.opsany.replica.repository;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.opsany.replica.domain.WorkOrderSatisfaction;

@Mapper
public interface WorkOrderSatisfactionRepository {

    int insert(WorkOrderSatisfaction satisfaction);

    int update(WorkOrderSatisfaction satisfaction);

    WorkOrderSatisfaction findByOrderNo(@Param("orderNo") String orderNo);

    List<WorkOrderSatisfaction> findAll();

    long count();
}
