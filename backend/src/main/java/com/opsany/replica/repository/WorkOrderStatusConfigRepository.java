package com.opsany.replica.repository;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.opsany.replica.domain.WorkOrderStatusConfig;

@Mapper
public interface WorkOrderStatusConfigRepository {

    int insert(WorkOrderStatusConfig config);

    int update(WorkOrderStatusConfig config);

    int deleteById(@Param("id") Long id);

    WorkOrderStatusConfig findById(@Param("id") Long id);

    WorkOrderStatusConfig findByStatusCode(@Param("statusCode") String statusCode);

    List<WorkOrderStatusConfig> findAll();

    List<WorkOrderStatusConfig> findEnabled();

    WorkOrderStatusConfig findInitialStatus();

    long count();
}
