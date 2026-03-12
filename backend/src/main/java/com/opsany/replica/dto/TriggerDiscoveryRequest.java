package com.opsany.replica.dto;

import lombok.Data;

@Data
public class TriggerDiscoveryRequest {

    private String subnet;
    private String protocol;
    private String snmpCommunity;
    private Integer port;
}
