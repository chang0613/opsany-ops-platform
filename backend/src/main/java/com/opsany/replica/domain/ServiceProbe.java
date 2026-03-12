package com.opsany.replica.domain;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceProbe {

    private Long id;
    private String probeCode;
    private String name;
    private String probeType;
    private String targetUrl;
    private String protocol;
    private Integer intervalSeconds;
    private Integer timeoutMs;
    private String status;
    private String lastResult;
    private LocalDateTime lastCheckedAt;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
