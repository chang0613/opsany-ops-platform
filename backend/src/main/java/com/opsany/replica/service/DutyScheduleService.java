package com.opsany.replica.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.opsany.replica.domain.AppUser;
import com.opsany.replica.domain.DutyGroup;
import com.opsany.replica.domain.DutyShift;
import com.opsany.replica.dto.SaveDutyGroupRequest;
import com.opsany.replica.dto.SaveDutyShiftRequest;
import com.opsany.replica.repository.AppUserRepository;
import com.opsany.replica.repository.DutyScheduleRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DutyScheduleService {

    private final DutyScheduleRepository dutyScheduleRepository;
    private final AppUserRepository appUserRepository;

    public List<DutyGroup> listGroups() {
        return dutyScheduleRepository.findAllGroups();
    }

    public List<DutyShift> listAllShifts() {
        return dutyScheduleRepository.findAllShifts();
    }

    public List<DutyShift> listByOwnerUsername(String username) {
        return dutyScheduleRepository.findByOwnerUsername(username);
    }

    public DutyGroup saveGroup(SaveDutyGroupRequest request, String fallbackUsername, String fallbackDisplayName) {
        AppUser owner = resolveOwner(request.getOwnerUsername(), fallbackUsername);
        DutyGroup existing = request.getId() == null ? null : dutyScheduleRepository.findGroupById(request.getId());
        DutyGroup group = DutyGroup.builder()
            .id(existing == null ? request.getId() : existing.getId())
            .name(defaultIfBlank(request.getName(), "未命名值班组"))
            .ownerUsername(owner == null ? defaultIfBlank(fallbackUsername, "admin") : owner.getUsername())
            .ownerDisplayName(owner == null ? defaultIfBlank(fallbackDisplayName, "管理员") : owner.getDisplayName())
            .members(request.getMembers() == null ? 0 : request.getMembers())
            .coverage(defaultIfBlank(request.getCoverage(), "工作日"))
            .description(request.getDescription())
            .build();

        if (existing == null) {
            dutyScheduleRepository.insertGroup(group);
            return group;
        }
        dutyScheduleRepository.updateGroup(group);
        return dutyScheduleRepository.findGroupById(group.getId());
    }

    public DutyShift saveShift(SaveDutyShiftRequest request, String fallbackUsername, String fallbackDisplayName) {
        DutyGroup group = dutyScheduleRepository.findGroupById(request.getGroupId());
        if (group == null) {
            throw new IllegalArgumentException("值班组不存在");
        }

        LocalDate dutyDate = LocalDate.parse(request.getDutyDate());
        AppUser owner = resolveOwner(request.getOwnerUsername(), fallbackUsername);
        DutyShift existing = request.getId() == null ? null : dutyScheduleRepository.findShiftById(request.getId());
        DutyShift shift = DutyShift.builder()
            .id(existing == null ? null : existing.getId())
            .groupId(group.getId())
            .groupName(group.getName())
            .dutyDate(dutyDate)
            .dateLabel(weekday(dutyDate))
            .shiftLabel(dutyDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + " " + weekday(dutyDate))
            .shiftTime(defaultIfBlank(request.getShiftTime(), "09:00 - 18:00"))
            .ownerUsername(owner == null ? defaultIfBlank(fallbackUsername, group.getOwnerUsername()) : owner.getUsername())
            .ownerDisplayName(owner == null ? defaultIfBlank(fallbackDisplayName, group.getOwnerDisplayName()) : owner.getDisplayName())
            .status(defaultIfBlank(request.getStatus(), "待值班"))
            .build();

        if (existing == null) {
            dutyScheduleRepository.insertShift(shift);
            return shift;
        }
        dutyScheduleRepository.updateShift(shift);
        return dutyScheduleRepository.findShiftById(shift.getId());
    }

    private AppUser resolveOwner(String requestOwnerUsername, String fallbackUsername) {
        if (StringUtils.hasText(requestOwnerUsername)) {
            AppUser owner = appUserRepository.findByUsername(requestOwnerUsername);
            if (owner != null) {
                return owner;
            }
        }
        if (StringUtils.hasText(fallbackUsername)) {
            return appUserRepository.findByUsername(fallbackUsername);
        }
        return null;
    }

    private String weekday(LocalDate dutyDate) {
        switch (dutyDate.getDayOfWeek()) {
            case MONDAY:
                return "周一";
            case TUESDAY:
                return "周二";
            case WEDNESDAY:
                return "周三";
            case THURSDAY:
                return "周四";
            case FRIDAY:
                return "周五";
            case SATURDAY:
                return "周六";
            default:
                return "周日";
        }
    }

    private String defaultIfBlank(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }
}
