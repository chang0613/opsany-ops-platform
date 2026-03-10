package com.opsany.replica.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.opsany.replica.dto.UserOption;
import com.opsany.replica.repository.AppUserRepository;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/workbench/users")
@RequiredArgsConstructor
public class UserOptionController {

    private final AppUserRepository appUserRepository;

    @GetMapping
    public List<UserOption> listUsers() {
        return appUserRepository.findAllUserOptions();
    }
}
