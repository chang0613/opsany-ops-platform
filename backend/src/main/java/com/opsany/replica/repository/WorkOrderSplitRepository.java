package com.opsany.replica.repository;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.opsany.replica.domain.WorkOrderSplit;

@Mapper
public interface WorkOrderSplitRepository {

    int insert(WorkOrderSplit split);

    WorkOrderSplit findById(@Param("id") Long id);

    WorkOrderSplit findByNewOrderNo(@Param("newOrderNo") String newOrderNo);

    List<WorkOrderSplit> findBySourceOrderNo(@Param("sourceOrderNo") String sourceOrderNo);

    long countBySourceOrderNo(@Param("sourceOrderNo") String sourceOrderNo);
}
