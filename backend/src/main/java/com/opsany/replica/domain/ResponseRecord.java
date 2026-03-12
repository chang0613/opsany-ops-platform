package com.opsany.replica.domain;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResponseRecord {
    private Long id;
    private String alertName;
    private String responder;
    private Integer responseMin;
    private Integer resolveMin;
    private String result;
    private LocalDateTime triggeredAt;
}
