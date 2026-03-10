package com.opsany.replica.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.opsany.replica.domain.WorkOrderProcessDefinition;
import com.opsany.replica.domain.WorkOrderProcessNode;
import com.opsany.replica.dto.SaveProcessDefinitionRequest;
import com.opsany.replica.security.AuthInterceptor;
import com.opsany.replica.security.SessionUser;
import com.opsany.replica.service.WorkOrderProcessService;

import javax.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/workbench/processes")
@RequiredArgsConstructor
public class ProcessController {

    private final WorkOrderProcessService workOrderProcessService;

    @GetMapping
    public List<WorkOrderProcessDefinition> list() {
        return workOrderProcessService.listDefinitions();
    }

    @GetMapping("/{processCode}/nodes")
    public List<WorkOrderProcessNode> listNodes(@PathVariable String processCode) {
        return workOrderProcessService.listNodes(processCode);
    }

    @PostMapping
    public WorkOrderProcessDefinition save(
        @RequestBody SaveProcessDefinitionRequest request,
        HttpServletRequest servletRequest
    ) {
        SessionUser sessionUser = (SessionUser) servletRequest.getAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE);
        return workOrderProcessService.saveDefinition(request, sessionUser.getDisplayName());
    }
}
