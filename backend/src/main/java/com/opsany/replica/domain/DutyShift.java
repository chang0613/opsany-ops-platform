package com.opsany.replica.domain;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DutyShift {

    private Long id;
    private Long groupId;
    private String groupName;
    private LocalDate dutyDate;
    private String dateLabel;
    private String shiftLabel;
    private String shiftTime;
    private String ownerUsername;
    private String ownerDisplayName;
    private String status;
}
