package com.opsany.replica.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SaveDutyShiftRequest {

    private Long id;
    private Long groupId;
    private String dutyDate;
    private String shiftTime;
    private String ownerUsername;
    private String status;
}
