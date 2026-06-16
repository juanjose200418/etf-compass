package com.etfcompass.backend.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
    @Email @NotBlank String email,
    @NotBlank @Pattern(regexp = "\\d{6}") String code,
    @NotBlank @Size(min = 8, max = 120) String newPassword
) {}
