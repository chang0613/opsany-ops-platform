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
public class ArchivePolicy {
    private Long id;
    private String policyCode;
    private String policyName;
    private String targetType;
    private Integer hotDays;
    private Integer warmDays;
    private Integer coldDays;
    private Integer compress;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
