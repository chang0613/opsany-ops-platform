package com.opsany.replica.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.opsany.replica.domain.DutyGroup;
import com.opsany.replica.domain.DutyShift;
import com.opsany.replica.repository.DutyScheduleRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DutyScheduleService {

    private final DutyScheduleRepository dutyScheduleRepository;

    public List<DutyGroup> listGroups() {
        return dutyScheduleRepository.findAllGroups();
    }

    public List<DutyShift> listAllShifts() {
        return dutyScheduleRepository.findAllShifts();
    }

    public List<DutyShift> listByOwnerUsername(String username) {
        return dutyScheduleRepository.findByOwnerUsername(username);
    }
}
