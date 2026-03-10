package com.opsany.replica.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DutyGroup {

    private Long id;
    private String name;
    private String ownerUsername;
    private String ownerDisplayName;
    private Integer members;
    private String coverage;
    private String description;
}
