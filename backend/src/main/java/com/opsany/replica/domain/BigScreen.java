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
public class BigScreen {
    private Long id;
    private String screenCode;
    private String name;
    private String resolution;
    private Integer charts;
    private Integer autoPlay;
    private Integer shared;
    private Integer visitToday;
    private LocalDateTime updatedAt;
    private LocalDateTime createdAt;
}
