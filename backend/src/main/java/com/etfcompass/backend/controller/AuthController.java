package com.etfcompass.backend.controller;

import com.etfcompass.backend.dto.ApiResponse;
import com.etfcompass.backend.dto.auth.AuthResponse;
import com.etfcompass.backend.dto.auth.ForgotPasswordRequest;
import com.etfcompass.backend.dto.auth.LoginRequest;
import com.etfcompass.backend.dto.auth.RegisterRequest;
import com.etfcompass.backend.dto.auth.ResetPasswordRequest;
import com.etfcompass.backend.dto.auth.UserResponse;
import com.etfcompass.backend.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
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
  ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
    return ResponseEntity.status(201).body(ApiResponse.created(authService.register(request)));
  }

  @PostMapping("/login")
  ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
    return ApiResponse.ok(authService.login(request));
  }

  @PostMapping("/forgot-password")
  ApiResponse<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
    authService.requestPasswordResetCode(request);
    return ApiResponse.ok(null);
  }

  @PostMapping("/reset-password")
  ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
    authService.resetPassword(request);
    return ApiResponse.ok(null);
  }

  @GetMapping("/me")
  ApiResponse<UserResponse> me(@AuthenticationPrincipal UserDetails userDetails) {
    return ApiResponse.ok(authService.me(userDetails.getUsername()));
  }
}
