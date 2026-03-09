package com.opsany.replica.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.opsany.replica.domain.AppUser;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByUsername(String username);
}
