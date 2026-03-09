package com.opsany.replica.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.opsany.replica.domain.WorkOrder;

public interface WorkOrderRepository extends JpaRepository<WorkOrder, Long> {

    List<WorkOrder> findTop10ByCreatorUsernameOrderByCreatedAtDesc(String creatorUsername);

    long countByCreatorUsername(String creatorUsername);
}
