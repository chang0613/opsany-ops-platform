package com.opsany.replica.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MenuConfigSaveRequest {

    private String menuCode;
    private String groupName;
    private Integer groupSortNo;
    private String label;
    private String route;
    private String icon;
    private Integer sortNo;
    private String permissionCode;
    private Boolean visible;
    private List<String> roleCodes;
}
