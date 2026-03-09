package com.opsany.replica.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.opsany.replica.domain.LoginAudit;

public interface LoginAuditRepository extends JpaRepository<LoginAudit, Long> {
}
