package com.opsany.replica.repository;

import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.opsany.replica.domain.WorkOrderHistory;

@Mapper
public interface WorkOrderHistoryRepository {

    @Insert({
        "insert into work_order_histories(",
        "order_no, action, from_status, to_status, from_node_code, to_node_code, operator_username,",
        "operator_display_name, comment, created_at",
        ") values (",
        "#{orderNo}, #{action}, #{fromStatus}, #{toStatus}, #{fromNodeCode}, #{toNodeCode}, #{operatorUsername},",
        "#{operatorDisplayName}, #{comment}, #{createdAt}",
        ")"
    })
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(WorkOrderHistory history);

    @Select({
        "select id, order_no, action, from_status, to_status, from_node_code, to_node_code, operator_username,",
        "operator_display_name, comment, created_at",
        "from work_order_histories where order_no = #{orderNo} order by created_at asc"
    })
    List<WorkOrderHistory> findByOrderNo(@Param("orderNo") String orderNo);
}
