package com.etfcompass.backend.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank(message = "El email es obligatorio")
    @Email(message = "Escribe un email valido")
    String email,
    @NotBlank(message = "La password es obligatoria")
    @Size(min = 8, max = 120, message = "La password debe tener entre 8 y 120 caracteres")
    String password,
    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 120, message = "El nombre no puede superar los 120 caracteres")
    String displayName
) {}
