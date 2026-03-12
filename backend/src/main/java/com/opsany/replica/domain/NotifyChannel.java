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
public class NotifyChannel {
    private Long id;
    private String channelCode;
    private String channelName;
    private String channelType;
    private String config;
    private Integer sentToday;
    private Integer failToday;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
