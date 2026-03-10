package com.opsany.replica.dto;

import java.util.List;

import com.opsany.replica.domain.WorkOrder;
import com.opsany.replica.domain.WorkOrderHistory;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkOrderDetailResponse {

    private WorkOrder order;
    private List<WorkOrderHistory> histories;
}
