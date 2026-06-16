package com.etfcompass.backend.dto.auth;

import com.etfcompass.backend.domain.Role;
import java.util.Set;
import java.util.UUID;

public record UserResponse(
    UUID id,
    String email,
    String displayName,
    Set<Role> roles
) {}
