package com.opsany.replica.dto;

import javax.validation.constraints.NotBlank;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransitionWorkOrderRequest {

    @NotBlank(message = "流转动作不能为空")
    private String action;

    private String comment;
}
