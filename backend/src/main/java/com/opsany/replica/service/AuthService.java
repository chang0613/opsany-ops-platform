package com.opsany.replica.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.opsany.replica.domain.AppUser;
import com.opsany.replica.domain.LoginAudit;
import com.opsany.replica.dto.LoginRequest;
import com.opsany.replica.dto.LoginResponse;
import com.opsany.replica.dto.UserProfile;
import com.opsany.replica.repository.AppUserRepository;
import com.opsany.replica.repository.LoginAuditRepository;
import com.opsany.replica.security.PasswordCodec;
import com.opsany.replica.security.SessionService;
import com.opsany.replica.security.SessionUser;

import javax.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AppUserRepository appUserRepository;
    private final LoginAuditRepository loginAuditRepository;
    private final MenuPermissionService menuPermissionService;
    private final PasswordCodec passwordCodec;
    private final SessionService sessionService;

    public LoginResponse login(LoginRequest request, HttpServletRequest servletRequest) {
        AppUser user = appUserRepository.findByUsername(request.getUsername());
        if (user == null || Boolean.FALSE.equals(user.getEnabled())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户名或密码错误");
        }

        if (!passwordCodec.matches(request.getPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户名或密码错误");
        }

        List<String> roleCodes = appUserRepository.findRoleCodes(user.getId());
        SessionUser sessionUser = new SessionUser(user.getId(), user.getUsername(), user.getDisplayName(), roleCodes);
        String token = sessionService.createSession(sessionUser);
        loginAuditRepository.insert(LoginAudit.builder()
            .userId(user.getId())
            .username(user.getUsername())
            .loginIp(resolveClientIp(servletRequest))
            .userAgent(resolveUserAgent(servletRequest))
            .loginAt(LocalDateTime.now())
            .build());
        appUserRepository.updateLastLoginAt(user.getId(), LocalDateTime.now());

        return new LoginResponse(
            token,
            new UserProfile(user.getId(), user.getUsername(), user.getDisplayName()),
            menuPermissionService.resolveLandingRoute(user.getId())
        );
    }

    private String resolveClientIp(HttpServletRequest servletRequest) {
        String forwarded = servletRequest.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.trim().isEmpty()) {
            return forwarded.split(",")[0].trim();
        }
        return servletRequest.getRemoteAddr();
    }

    private String resolveUserAgent(HttpServletRequest servletRequest) {
        String userAgent = servletRequest.getHeader("User-Agent");
        return userAgent == null ? "Unknown" : userAgent;
    }
}
