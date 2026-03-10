package com.opsany.replica.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlatformNavigationItem {

    private Long id;
    private String itemCode;
    private String groupCode;
    private String name;
    private String icon;
    private String creatorUsername;
    private String creatorDisplayName;
    private String link;
    private Boolean mobileVisible;
    private String description;
    private Integer sortNo;
    private Boolean enabled;
}
