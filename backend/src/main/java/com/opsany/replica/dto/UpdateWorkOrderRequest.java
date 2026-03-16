package com.opsany.replica.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateWorkOrderRequest {

    private String title;
    private String type;
    private String priority;
    private String serviceName;
    private String description;
}
