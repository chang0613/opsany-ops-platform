package com.opsany.replica.messaging;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkOrderCreatedEvent {

    private Long orderId;
    private String orderNo;
    private String title;
    private String type;
    private String creatorUsername;
    private String creatorDisplayName;
    private String priority;
    private LocalDateTime createdAt;
}
