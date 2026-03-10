package com.opsany.replica.dto;

import lombok.Data;

@Data
public class NavigationItemSaveRequest {

    private String itemCode;
    private String groupCode;
    private String name;
    private String icon;
    private String link;
    private Boolean mobileVisible;
    private String description;
    private Integer sortNo;
    private Boolean enabled;
}
