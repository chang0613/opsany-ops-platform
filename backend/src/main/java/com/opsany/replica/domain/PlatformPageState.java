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
public class PlatformPageState {

    private Long id;
    private String platformKey;
    private String pageKey;
    private String stateJson;
    private String updatedBy;
    private LocalDateTime updatedAt;
}
