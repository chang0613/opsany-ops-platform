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
public class ArchiveHistory {
    private Long id;
    private String policyCode;
    private String policy;
    private String dataSize;
    private Integer duration;
    private String status;
    private LocalDateTime executedAt;
}
