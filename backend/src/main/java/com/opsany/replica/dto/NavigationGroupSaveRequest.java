package com.opsany.replica.dto;

import lombok.Data;

@Data
public class NavigationGroupSaveRequest {

    private String groupCode;
    private String title;
    private Integer sortNo;
}
