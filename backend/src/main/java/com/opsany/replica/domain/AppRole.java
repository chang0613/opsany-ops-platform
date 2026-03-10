package com.opsany.replica.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppRole {

    private String roleCode;
    private String roleName;
    private String description;
    private Integer sortNo;
}
