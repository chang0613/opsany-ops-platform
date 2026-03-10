package com.opsany.replica.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlatformNavigationGroup {

    private Long id;
    private String groupCode;
    private String title;
    private Integer sortNo;
}
