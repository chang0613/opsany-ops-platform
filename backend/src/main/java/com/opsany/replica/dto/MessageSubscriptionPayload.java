package com.opsany.replica.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageSubscriptionPayload {

    private String messageType;
    private String source;
    private Boolean siteEnabled;
    private Boolean smsEnabled;
    private Boolean mailEnabled;
    private Boolean wxEnabled;
    private Boolean dingEnabled;
}
