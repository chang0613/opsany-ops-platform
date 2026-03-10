package com.opsany.replica.repository;

import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.opsany.replica.domain.WorkOrder;

@Mapper
public interface WorkOrderRepository {

    @Insert({
        "insert into work_orders(",
        "order_no, title, type, creator_username, creator_display_name, progress, status, priority,",
        "service_name, description, estimated_at, created_at, updated_at, process_code, current_node_code,",
        "current_node_name, current_handler",
        ") values (",
        "#{orderNo}, #{title}, #{type}, #{creatorUsername}, #{creatorDisplayName}, #{progress}, #{status}, #{priority},",
        "#{serviceName}, #{description}, #{estimatedAt}, #{createdAt}, #{updatedAt}, #{processCode}, #{currentNodeCode},",
        "#{currentNodeName}, #{currentHandler}",
        ")"
    })
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(WorkOrder workOrder);

    @Update({
        "update work_orders set",
        "progress = #{progress},",
        "status = #{status},",
        "priority = #{priority},",
        "estimated_at = #{estimatedAt},",
        "updated_at = #{updatedAt},",
        "current_node_code = #{currentNodeCode},",
        "current_node_name = #{currentNodeName},",
        "current_handler = #{currentHandler}",
        "where order_no = #{orderNo}"
    })
    int updateFlowState(WorkOrder workOrder);

    @Select({
        "select id, order_no, title, type, creator_username, creator_display_name, progress, status, priority,",
        "service_name, description, estimated_at, created_at, updated_at, process_code, current_node_code,",
        "current_node_name, current_handler",
        "from work_orders where order_no = #{orderNo} limit 1"
    })
    WorkOrder findByOrderNo(@Param("orderNo") String orderNo);

    @Select({
        "select id, order_no, title, type, creator_username, creator_display_name, progress, status, priority,",
        "service_name, description, estimated_at, created_at, updated_at, process_code, current_node_code,",
        "current_node_name, current_handler",
        "from work_orders where creator_username = #{creatorUsername}",
        "order by created_at desc limit 10"
    })
    List<WorkOrder> findTop10ByCreatorUsernameOrderByCreatedAtDesc(@Param("creatorUsername") String creatorUsername);

    @Select({
        "select id, order_no, title, type, creator_username, creator_display_name, progress, status, priority,",
        "service_name, description, estimated_at, created_at, updated_at, process_code, current_node_code,",
        "current_node_name, current_handler",
        "from work_orders order by created_at desc limit 20"
    })
    List<WorkOrder> findTop20OrderByCreatedAtDesc();

    @Select("select count(1) from work_orders where creator_username = #{creatorUsername}")
    long countByCreatorUsername(@Param("creatorUsername") String creatorUsername);

    @Select("select count(1) from work_orders")
    long count();
}
