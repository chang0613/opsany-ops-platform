package com.opsany.replica.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Delete;

import com.opsany.replica.domain.WorkOrderProcessDefinition;
import com.opsany.replica.domain.WorkOrderProcessNode;

@Mapper
public interface WorkOrderProcessRepository {

    @Select("select count(1) from work_order_processes where process_code = #{processCode}")
    long countByProcessCode(@Param("processCode") String processCode);

    @Insert({
        "insert into work_order_processes(",
        "process_code, name, category, version, status, owner, creator, updater, updated_at, description, definition_json",
        ") values (",
        "#{processCode}, #{name}, #{category}, #{version}, #{status}, #{owner}, #{creator}, #{updater}, #{updatedAt},",
        "#{description}, #{definitionJson}",
        ")"
    })
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertDefinition(WorkOrderProcessDefinition definition);

    @Update({
        "update work_order_processes set name = #{name}, category = #{category}, version = #{version}, status = #{status},",
        "owner = #{owner}, creator = #{creator}, updater = #{updater}, updated_at = #{updatedAt}, description = #{description},",
        "definition_json = #{definitionJson}",
        "where process_code = #{processCode}"
    })
    int updateDefinition(WorkOrderProcessDefinition definition);

    @Select({
        "select id, process_code, name, category, version, status, owner, creator, updater, updated_at, description, definition_json",
        "from work_order_processes order by updated_at desc"
    })
    List<WorkOrderProcessDefinition> findAllDefinitions();

    @Select({
        "select id, process_code, name, category, version, status, owner, creator, updater, updated_at, description, definition_json",
        "from work_order_processes where process_code = #{processCode} limit 1"
    })
    WorkOrderProcessDefinition findByProcessCode(@Param("processCode") String processCode);

    @Select("select count(1) from work_order_process_nodes where process_code = #{processCode}")
    long countNodesByProcessCode(@Param("processCode") String processCode);

    @Insert({
        "insert into work_order_process_nodes(",
        "process_code, node_code, node_name, node_type, sort_no, assignee_role, next_approve_node, next_reject_node",
        ") values (",
        "#{processCode}, #{nodeCode}, #{nodeName}, #{nodeType}, #{sortNo}, #{assigneeRole}, #{nextApproveNode}, #{nextRejectNode}",
        ")"
    })
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertNode(WorkOrderProcessNode node);

    @Delete("delete from work_order_process_nodes where process_code = #{processCode}")
    int deleteNodesByProcessCode(@Param("processCode") String processCode);

    @Select({
        "select id, process_code, node_code, node_name, node_type, sort_no, assignee_role, next_approve_node, next_reject_node",
        "from work_order_process_nodes where process_code = #{processCode} order by sort_no asc"
    })
    List<WorkOrderProcessNode> findNodesByProcessCode(@Param("processCode") String processCode);

    @Select({
        "select id, process_code, node_code, node_name, node_type, sort_no, assignee_role, next_approve_node, next_reject_node",
        "from work_order_process_nodes",
        "where process_code = #{processCode} and node_code = #{nodeCode} limit 1"
    })
    WorkOrderProcessNode findNode(@Param("processCode") String processCode, @Param("nodeCode") String nodeCode);

    @Select({
        "select id, process_code, node_code, node_name, node_type, sort_no, assignee_role, next_approve_node, next_reject_node",
        "from work_order_process_nodes",
        "where process_code = #{processCode}",
        "order by sort_no asc limit 1"
    })
    WorkOrderProcessNode findStartNode(@Param("processCode") String processCode);

    @Update("update work_order_processes set updated_at = #{updatedAt} where process_code = #{processCode}")
    int touchDefinition(@Param("processCode") String processCode, @Param("updatedAt") LocalDateTime updatedAt);
}
