package com.opsany.replica.repository;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.opsany.replica.domain.WorkOrderReminder;

@Mapper
public interface WorkOrderReminderRepository {

    int insert(WorkOrderReminder reminder);

    int deleteById(@Param("id") Long id);

    WorkOrderReminder findById(@Param("id") Long id);

    List<WorkOrderReminder> findByOrderNo(@Param("orderNo") String orderNo);

    List<WorkOrderReminder> findByOrderNoOrderByCreatedAtDesc(@Param("orderNo") String orderNo);

    long countByOrderNo(@Param("orderNo") String orderNo);

    long countTodayByUsername(@Param("username") String username);
}
