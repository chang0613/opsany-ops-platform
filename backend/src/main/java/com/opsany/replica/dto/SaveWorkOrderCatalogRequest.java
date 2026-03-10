package com.opsany.replica.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SaveWorkOrderCatalogRequest {

    private String catalogCode;
    private String name;
    private String category;
    private String type;
    private String scope;
    private Boolean online;
    private String processCode;
    private String slaName;
    private String ownerUsername;
    private String description;
    private Integer sortNo;
}
