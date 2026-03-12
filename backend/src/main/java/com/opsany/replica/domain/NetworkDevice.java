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
public class NetworkDevice {

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
    private LocalDateTime lastCollectedAt;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
