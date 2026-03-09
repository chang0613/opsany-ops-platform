package com.opsany.replica.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.opsany.replica.domain.TaskRecord;

public interface TaskRecordRepository extends JpaRepository<TaskRecord, Long> {

    List<TaskRecord> findTop10ByOrderByCreatedAtDesc();

    long countByAssigneeAndStatus(String assignee, String status);

    long countByAssigneeAndStatusNot(String assignee, String status);

    long countByCreator(String creator);
}
