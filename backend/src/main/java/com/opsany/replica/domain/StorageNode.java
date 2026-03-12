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
public class StorageNode {
    private Long id;
    private String nodeCode;
    private String node;
    private String role;
    private String used;
    private String total;
    private String writeRate;
    private String queryRate;
    private String status;
    private LocalDateTime updatedAt;
}
