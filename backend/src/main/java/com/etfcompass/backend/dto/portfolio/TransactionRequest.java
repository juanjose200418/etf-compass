package com.etfcompass.backend.dto.portfolio;

import com.etfcompass.backend.domain.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionRequest(
    @NotNull TransactionType type,
    @NotNull LocalDate transactionDate,
    @DecimalMin("0.0") BigDecimal quantity,
    @DecimalMin("0.0") BigDecimal price,
    @DecimalMin("0.0") BigDecimal fees,
    @Pattern(regexp = "^[A-Z]{3}$") String currency
) {}
