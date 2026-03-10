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
public class TaskRecord {

    private Long id;

    private String taskNo;

    private String title;

    private String source;

    private String ticket;

    private String status;

    private String assignee;

    private String priority;

    private String creator;

    private LocalDateTime createdAt;

    private String orderNo;

    private String nodeCode;

    private LocalDateTime completedAt;
}
