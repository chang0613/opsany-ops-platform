package com.opsany.replica.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SaveDutyGroupRequest {

    private Long id;
    private String name;
    private String ownerUsername;
    private Integer members;
    private String coverage;
    private String description;
}
