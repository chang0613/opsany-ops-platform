package com.opsany.replica.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserOption {

    private Long id;
    private String username;
    private String displayName;
}
