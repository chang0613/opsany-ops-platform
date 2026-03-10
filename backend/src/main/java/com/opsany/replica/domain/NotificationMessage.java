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
public class NotificationMessage {

    private Long id;

    private String title;

    private String messageType;

    private LocalDateTime sentAt;

    private boolean read;

    private String recipientUsername;

    private String sourceType;

    private String sourceId;
}
