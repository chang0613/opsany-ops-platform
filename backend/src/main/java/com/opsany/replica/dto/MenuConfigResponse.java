package com.opsany.replica.dto;

import java.util.List;

import com.opsany.replica.domain.AppRole;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MenuConfigResponse {

    private List<AppRole> roles;
    private List<MenuConfigItem> menus;
}
