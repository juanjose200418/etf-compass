package com.etfcompass.backend.dto.portfolio;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreatePortfolioRequest(
    @NotBlank @Size(max = 120) String name,
    @Pattern(regexp = "^[A-Z]{3}$") String baseCurrency
) {}
