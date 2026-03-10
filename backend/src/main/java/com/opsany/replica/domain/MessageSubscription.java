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
public class MessageSubscription {

    private Long id;
    private String username;
    private String messageType;
    private String source;
    private Boolean siteEnabled;
    private Boolean smsEnabled;
    private Boolean mailEnabled;
    private Boolean wxEnabled;
    private Boolean dingEnabled;
    private LocalDateTime updatedAt;
}
