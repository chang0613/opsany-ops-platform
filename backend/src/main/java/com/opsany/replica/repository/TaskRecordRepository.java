package com.opsany.replica.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.opsany.replica.domain.TaskRecord;

@Mapper
public interface TaskRecordRepository {

    @Select({
        "select id, task_no, title, source, ticket, status, assignee, priority, creator, created_at,",
        "order_no, node_code, completed_at",
        "from task_records order by created_at desc limit 10"
    })
    List<TaskRecord> findTop10ByOrderByCreatedAtDesc();

    @Select("select count(1) from task_records where assignee = #{assignee} and status = #{status}")
    long countByAssigneeAndStatus(@Param("assignee") String assignee, @Param("status") String status);

    @Select("select count(1) from task_records where assignee = #{assignee} and status <> #{status}")
    long countByAssigneeAndStatusNot(@Param("assignee") String assignee, @Param("status") String status);

    @Select("select count(1) from task_records where creator = #{creator}")
    long countByCreator(@Param("creator") String creator);

    @Select("select count(1) from task_records")
    long count();

    @Select("select count(1) from task_records where order_no = #{orderNo} and status <> '已完成'")
    long countOpenByOrderNo(@Param("orderNo") String orderNo);

    @Insert({
        "insert into task_records(",
        "task_no, title, source, ticket, status, assignee, priority, creator, created_at, order_no, node_code, completed_at",
        ") values (",
        "#{taskNo}, #{title}, #{source}, #{ticket}, #{status}, #{assignee}, #{priority}, #{creator}, #{createdAt},",
        "#{orderNo}, #{nodeCode}, #{completedAt}",
        ")"
    })
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(TaskRecord taskRecord);

    @Update({
        "update task_records set status = '已完成', completed_at = #{completedAt}",
        "where order_no = #{orderNo} and status <> '已完成'"
    })
    int closeOpenTasks(@Param("orderNo") String orderNo, @Param("completedAt") LocalDateTime completedAt);
}
