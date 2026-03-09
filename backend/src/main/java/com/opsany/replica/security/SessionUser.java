package com.opsany.replica.security;

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
}
