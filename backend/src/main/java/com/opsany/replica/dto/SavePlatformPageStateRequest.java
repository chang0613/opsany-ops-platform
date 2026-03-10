package com.opsany.replica.dto;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SavePlatformPageStateRequest {

    private String platformKey;
    private String pageKey;
    private JsonNode tableStates;
}
