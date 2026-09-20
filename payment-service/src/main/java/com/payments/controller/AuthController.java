package com.payments.controller;

import com.payments.dto.request.LoginRequest;
import com.payments.dto.request.LogoutRequest;
import com.payments.dto.request.RefreshTokenRequest;
import com.payments.dto.request.RegisterRequest;
import com.payments.dto.response.ApiResponse;
import com.payments.dto.response.AuthResponse;
import com.payments.dto.response.UserResponse;
import com.payments.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> register(@Valid @RequestBody RegisterRequest request) {
        UserResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout(HttpServletRequest request,
                                                     @Valid @RequestBody LogoutRequest logoutRequest) {
        String authHeader = request.getHeader("Authorization");
        String accessToken = authHeader.substring(7);

        authService.logout(accessToken, logoutRequest);
        return ResponseEntity.ok(ApiResponse.ok("Logout successful."));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = authService.refresh(request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
