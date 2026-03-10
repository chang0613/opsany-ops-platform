package com.opsany.replica.security;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SessionUser {

    private Long userId;
    private String username;
    private String displayName;
    private List<String> roleCodes;
}
