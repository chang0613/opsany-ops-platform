package com.opsany.replica.controller;

import java.util.List;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.opsany.replica.domain.WorkOrder;
import com.opsany.replica.dto.CreateWorkOrderRequest;
import com.opsany.replica.dto.CreateWorkOrderResponse;
import com.opsany.replica.dto.TransitionWorkOrderRequest;
import com.opsany.replica.dto.WorkOrderDetailResponse;
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

    @GetMapping
    public List<WorkOrder> listOrders(HttpServletRequest servletRequest) {
        SessionUser sessionUser = (SessionUser) servletRequest.getAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE);
        return orderLifecycleService.listRecentOrders(sessionUser);
    }

    @GetMapping("/{orderNo}")
    public WorkOrderDetailResponse detail(@PathVariable String orderNo) {
        return orderLifecycleService.getOrderDetail(orderNo);
    }

    @PostMapping
    public CreateWorkOrderResponse createOrder(
        @Validated @RequestBody CreateWorkOrderRequest request,
        HttpServletRequest servletRequest
    ) {
        SessionUser sessionUser = (SessionUser) servletRequest.getAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE);
        WorkOrder workOrder = orderLifecycleService.createOrder(request, sessionUser);
        return new CreateWorkOrderResponse(workOrder.getOrderNo(), workOrder.getTitle(), workOrder.getStatus());
    }

    @PostMapping("/{orderNo}/transition")
    public CreateWorkOrderResponse transition(
        @PathVariable String orderNo,
        @Validated @RequestBody TransitionWorkOrderRequest request,
        HttpServletRequest servletRequest
    ) {
        SessionUser sessionUser = (SessionUser) servletRequest.getAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE);
        WorkOrder workOrder = orderLifecycleService.transitionOrder(orderNo, request, sessionUser);
        return new CreateWorkOrderResponse(workOrder.getOrderNo(), workOrder.getTitle(), workOrder.getStatus());
    }
}
