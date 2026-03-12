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
public class OnCallSchedule {
    private Long id;
    private LocalDate dutyDate;
    private String weekday;
    private String groupName;
    private String person;
    private String shift;
    private String status;
}
