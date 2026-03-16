package com.opsany.replica.repository;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.opsany.replica.domain.WorkOrderExt;

@Mapper
public interface WorkOrderExtRepository {

    int insert(WorkOrderExt ext);

    int update(WorkOrderExt ext);

    WorkOrderExt findByOrderNo(@Param("orderNo") String orderNo);

    int updateSolution(@Param("orderNo") String orderNo, @Param("solution") String solution);

    int updateReminderCount(@Param("orderNo") String orderNo);

    List<WorkOrderExt> findAll();

    long count();
}
