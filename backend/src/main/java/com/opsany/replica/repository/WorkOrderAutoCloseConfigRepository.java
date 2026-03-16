package com.opsany.replica.repository;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.opsany.replica.domain.WorkOrderAutoCloseConfig;

@Mapper
public interface WorkOrderAutoCloseConfigRepository {

    int insert(WorkOrderAutoCloseConfig config);

    int update(WorkOrderAutoCloseConfig config);

    int deleteById(@Param("id") Long id);

    WorkOrderAutoCloseConfig findById(@Param("id") Long id);

    WorkOrderAutoCloseConfig findByStatusAndEnabled(@Param("status") String status);

    List<WorkOrderAutoCloseConfig> findAll();

    List<WorkOrderAutoCloseConfig> findEnabled();

    long count();
}
