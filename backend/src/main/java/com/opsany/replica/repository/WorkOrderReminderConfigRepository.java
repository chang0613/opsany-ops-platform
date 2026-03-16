package com.opsany.replica.repository;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.opsany.replica.domain.WorkOrderReminderConfig;

@Mapper
public interface WorkOrderReminderConfigRepository {

    int insert(WorkOrderReminderConfig config);

    int update(WorkOrderReminderConfig config);

    int deleteById(@Param("id") Long id);

    WorkOrderReminderConfig findById(@Param("id") Long id);

    WorkOrderReminderConfig findByConfigKey(@Param("configKey") String configKey);

    List<WorkOrderReminderConfig> findAll();

    long count();
}
