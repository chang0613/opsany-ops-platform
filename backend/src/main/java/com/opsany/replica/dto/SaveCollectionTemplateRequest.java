package com.opsany.replica.dto;

import lombok.Data;

@Data
public class SaveCollectionTemplateRequest {

    private Long id;
    private String templateCode;
    private String name;
    private String deviceType;
    private String protocol;
    private String metricsJson;
    private Integer intervalSeconds;
    private Boolean enabled;
    private String description;
}
