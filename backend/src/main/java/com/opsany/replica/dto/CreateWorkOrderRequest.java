package com.opsany.replica.dto;

import javax.validation.constraints.NotBlank;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateWorkOrderRequest {

    @NotBlank(message = "工单标题不能为空")
    private String title;

    @NotBlank(message = "工单类型不能为空")
    private String type;

    private String catalogCode;
    private String processCode;
    private String serviceName;
    private String description;
    private String priority;
}
