package com.opsany.replica.service.ai;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AiProviderMessage {

    private final String role;

    private final String content;
}

