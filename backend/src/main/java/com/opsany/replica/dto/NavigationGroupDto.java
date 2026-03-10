package com.opsany.replica.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NavigationGroupDto {

    private Long id;
    private String groupCode;
    private String title;
    private Integer sortNo;
    private List<NavigationItemDto> rows;
}
