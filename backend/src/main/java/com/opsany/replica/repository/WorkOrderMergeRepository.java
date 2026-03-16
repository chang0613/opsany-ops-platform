package com.opsany.replica.repository;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.opsany.replica.domain.WorkOrderMerge;

@Mapper
public interface WorkOrderMergeRepository {

    int insert(WorkOrderMerge merge);

    WorkOrderMerge findById(@Param("id") Long id);

    WorkOrderMerge findByMergedOrderNo(@Param("mergedOrderNo") String mergedOrderNo);

    List<WorkOrderMerge> findByMasterOrderNo(@Param("masterOrderNo") String masterOrderNo);

    long countByMasterOrderNo(@Param("masterOrderNo") String masterOrderNo);
}
