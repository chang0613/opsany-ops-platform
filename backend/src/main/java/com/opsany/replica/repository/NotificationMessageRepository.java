package com.opsany.replica.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.opsany.replica.domain.NotificationMessage;

public interface NotificationMessageRepository extends JpaRepository<NotificationMessage, Long> {

    List<NotificationMessage> findTop10ByOrderBySentAtDesc();

    List<NotificationMessage> findTop3ByOrderBySentAtDesc();
}
