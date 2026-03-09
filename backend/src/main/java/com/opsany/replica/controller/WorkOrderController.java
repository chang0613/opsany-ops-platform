package com.opsany.replica.controller;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.opsany.replica.domain.WorkOrder;
import com.opsany.replica.dto.CreateWorkOrderRequest;
import com.opsany.replica.dto.CreateWorkOrderResponse;
import com.opsany.replica.security.AuthInterceptor;
import com.opsany.replica.security.SessionUser;
import com.opsany.replica.service.OrderLifecycleService;

import javax.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/workbench/orders")
@RequiredArgsConstructor
public class WorkOrderController {

    private final OrderLifecycleService orderLifecycleService;

    @PostMapping
    public CreateWorkOrderResponse createOrder(
        @Validated @RequestBody CreateWorkOrderRequest request,
        HttpServletRequest servletRequest
    ) {
        SessionUser sessionUser = (SessionUser) servletRequest.getAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE);
        WorkOrder workOrder = orderLifecycleService.createOrder(request, sessionUser);
        return new CreateWorkOrderResponse(workOrder.getOrderNo(), workOrder.getTitle(), workOrder.getStatus());
    }
}
