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
public class Dashboard {
    private Long id;
    private String dashboardCode;
    private String name;
    private String category;
    private Integer charts;
    private String creator;
    private Integer shared;
    private Integer visitToday;
    private LocalDateTime updatedAt;
    private LocalDateTime createdAt;
}
