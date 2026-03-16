package com.opsany.replica.repository;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.opsany.replica.domain.WorkOrderHistory;

@Mapper
public interface WorkOrderHistoryRepository {

    int insert(WorkOrderHistory history);

    List<WorkOrderHistory> findByOrderNo(@Param("orderNo") String orderNo);

    List<WorkOrderHistory> findByOrderNoOrderByCreatedAtDesc(@Param("orderNo") String orderNo);

    long countByOrderNo(@Param("orderNo") String orderNo);
}
