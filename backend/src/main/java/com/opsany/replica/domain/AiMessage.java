package com.opsany.replica.domain;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiMessage {

    private Long id;

    private Long conversationId;

    private String role;

    private String content;

    private LocalDateTime createdAt;
}

