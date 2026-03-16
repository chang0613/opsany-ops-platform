package com.opsany.replica.repository;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.opsany.replica.domain.WorkOrderTransitionPermission;

@Mapper
public interface WorkOrderTransitionPermissionRepository {

    int insert(WorkOrderTransitionPermission permission);

    int deleteById(@Param("id") Long id);

    WorkOrderTransitionPermission findById(@Param("id") Long id);

    List<WorkOrderTransitionPermission> findByFromStatus(@Param("fromStatus") String fromStatus);

    List<WorkOrderTransitionPermission> findByRoleCode(@Param("roleCode") String roleCode);

    List<WorkOrderTransitionPermission> findAll();

    long count();
}
