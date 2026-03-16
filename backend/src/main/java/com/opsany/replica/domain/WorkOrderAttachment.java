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
public class WorkOrderAttachment {

    private Long id;

    private String orderNo;

    private String fileName;

    private String filePath;

    private Long fileSize;

    private String fileType;

    private String fileExtension;

    private String uploadUsername;

    private String uploadDisplayName;

    private LocalDateTime createdAt;
}
