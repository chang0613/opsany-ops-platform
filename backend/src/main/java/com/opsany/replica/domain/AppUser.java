package com.opsany.replica.domain;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppUser {

    private Long id;

    private String username;

    private String displayName;

    private String passwordHash;

    private String status;

    private LocalDateTime createdAt;

    private Boolean enabled;

    private LocalDateTime lastLoginAt;
}
