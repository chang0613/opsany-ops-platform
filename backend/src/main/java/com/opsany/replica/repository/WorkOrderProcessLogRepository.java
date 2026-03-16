package com.opsany.replica.repository;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.opsany.replica.domain.WorkOrderProcessLog;

@Mapper
public interface WorkOrderProcessLogRepository {

    int insert(WorkOrderProcessLog log);

    int deleteById(@Param("id") Long id);

    WorkOrderProcessLog findById(@Param("id") Long id);

    List<WorkOrderProcessLog> findByOrderNo(@Param("orderNo") String orderNo);

    List<WorkOrderProcessLog> findByOrderNoAndLogType(@Param("orderNo") String orderNo, @Param("logType") String logType);

    long countByOrderNo(@Param("orderNo") String orderNo);
}
