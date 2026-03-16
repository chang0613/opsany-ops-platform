package com.opsany.replica.repository;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.opsany.replica.domain.WorkOrder;

@Mapper
public interface WorkOrderRepository {

    int insert(WorkOrder workOrder);

    int updateFlowState(WorkOrder workOrder);

    int update(WorkOrder workOrder);

    int deleteByOrderNo(@Param("orderNo") String orderNo);

    WorkOrder findByOrderNo(@Param("orderNo") String orderNo);

    List<WorkOrder> findTop10ByCreatorUsernameOrderByCreatedAtDesc(@Param("creatorUsername") String creatorUsername);

    List<WorkOrder> findTop20OrderByCreatedAtDesc();

    List<WorkOrder> findAll(@Param("creatorUsername") String creatorUsername,
                           @Param("status") String status,
                           @Param("priority") String priority,
                           @Param("serviceName") String serviceName,
                           @Param("searchKeyword") String searchKeyword);

    List<WorkOrder> findPaged(@Param("creatorUsername") String creatorUsername,
                            @Param("status") String status,
                            @Param("priority") String priority,
                            @Param("serviceName") String serviceName,
                            @Param("searchKeyword") String searchKeyword,
                            @Param("offset") int offset,
                            @Param("limit") int limit);

    long countByCreatorUsername(@Param("creatorUsername") String creatorUsername);

    long countByServiceName(@Param("serviceName") String serviceName);

    long count();

    long countByCondition(@Param("creatorUsername") String creatorUsername,
                         @Param("status") String status,
                         @Param("priority") String priority);
}
