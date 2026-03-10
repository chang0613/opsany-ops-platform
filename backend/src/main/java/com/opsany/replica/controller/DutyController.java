package com.opsany.replica.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.opsany.replica.domain.DutyGroup;
import com.opsany.replica.domain.DutyShift;
import com.opsany.replica.dto.SaveDutyGroupRequest;
import com.opsany.replica.dto.SaveDutyShiftRequest;
import com.opsany.replica.security.AuthInterceptor;
import com.opsany.replica.security.SessionUser;
import com.opsany.replica.service.DutyScheduleService;
import com.opsany.replica.service.PlatformBootstrapService;

import javax.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/workbench/duty")
@RequiredArgsConstructor
public class DutyController {

    private final DutyScheduleService dutyScheduleService;
    private final PlatformBootstrapService platformBootstrapService;

    @GetMapping("/groups")
    public List<DutyGroup> groups() {
        return dutyScheduleService.listGroups();
    }

    @GetMapping("/shifts")
    public List<DutyShift> shifts() {
        return dutyScheduleService.listAllShifts();
    }

    @PostMapping("/groups")
    public DutyGroup saveGroup(@RequestBody SaveDutyGroupRequest request, HttpServletRequest servletRequest) {
        SessionUser sessionUser = (SessionUser) servletRequest.getAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE);
        DutyGroup group = dutyScheduleService.saveGroup(request, sessionUser.getUsername(), sessionUser.getDisplayName());
        platformBootstrapService.evictAllBootstrapCaches();
        return group;
    }

    @PostMapping("/shifts")
    public DutyShift saveShift(@RequestBody SaveDutyShiftRequest request, HttpServletRequest servletRequest) {
        SessionUser sessionUser = (SessionUser) servletRequest.getAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE);
        DutyShift shift = dutyScheduleService.saveShift(request, sessionUser.getUsername(), sessionUser.getDisplayName());
        platformBootstrapService.evictAllBootstrapCaches();
        return shift;
    }
}
