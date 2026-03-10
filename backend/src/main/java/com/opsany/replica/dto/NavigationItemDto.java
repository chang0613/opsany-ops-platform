package com.opsany.replica.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NavigationItemDto {

    private Long id;
    private String itemCode;
    private String groupCode;
    private String groupTitle;
    private Integer groupSortNo;
    private String name;
    private String icon;
    private String creatorUsername;
    private String creatorDisplayName;
    private String link;
    private Boolean mobileVisible;
    private String description;
    private Integer sortNo;
    private Boolean enabled;
    private Boolean favorite;
}
