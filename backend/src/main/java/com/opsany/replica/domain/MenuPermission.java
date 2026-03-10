package com.opsany.replica.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MenuPermission {

    private Long id;
    private String menuCode;
    private String groupName;
    private Integer groupSortNo;
    private String label;
    private String route;
    private String icon;
    private Integer sortNo;
    private String permissionCode;
    private Boolean visible;
}
