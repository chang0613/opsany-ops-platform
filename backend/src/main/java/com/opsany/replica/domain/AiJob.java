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
public class AiJob {

    private Long id;

    private String jobType;

    private String status;

    private String ownerUsername;

    private String inputJson;

    private String resultJson;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}

