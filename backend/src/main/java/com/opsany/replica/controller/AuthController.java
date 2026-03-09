package com.opsany.replica.controller;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.opsany.replica.dto.LoginRequest;
import com.opsany.replica.dto.LoginResponse;
import com.opsany.replica.dto.UserProfile;
import com.opsany.replica.security.AuthInterceptor;
import com.opsany.replica.security.SessionUser;
import com.opsany.replica.service.AuthService;

import javax.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public LoginResponse login(@Validated @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        return authService.login(request, servletRequest);
    }

    @GetMapping("/me")
    public UserProfile me(HttpServletRequest request) {
        SessionUser sessionUser = (SessionUser) request.getAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE);
        return new UserProfile(sessionUser.getUserId(), sessionUser.getUsername(), sessionUser.getDisplayName());
    }
}
