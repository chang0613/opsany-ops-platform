package com.opsany.replica.dto;

import lombok.Data;

@Data
public class SaveNetworkDeviceRequest {

    private Long id;
    private String deviceCode;
    private String name;
    private String ip;
    private String deviceType;
    private String vendor;
    private String protocol;
    private String snmpCommunity;
    private String snmpVersion;
    private String sshUsername;
    private Integer port;
    private String status;
    private String description;
}
