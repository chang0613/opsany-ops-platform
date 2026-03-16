package com.opsany.replica.repository;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.opsany.replica.domain.WorkOrderResource;

@Mapper
public interface WorkOrderResourceRepository {

    int insert(WorkOrderResource resource);

    int deleteById(@Param("id") Long id);

    int deleteByOrderNo(@Param("orderNo") String orderNo);

    WorkOrderResource findById(@Param("id") Long id);

    List<WorkOrderResource> findByOrderNo(@Param("orderNo") String orderNo);

    List<WorkOrderResource> findByResourceType(@Param("resourceType") String resourceType);

    long countByOrderNo(@Param("orderNo") String orderNo);
}
