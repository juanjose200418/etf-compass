package com.etfcompass.backend.dto.portfolio;

import com.etfcompass.backend.domain.Broker;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;

public record PositionRequest(
    @NotBlank String ticker,
    Broker broker,
    String externalSymbol,
    @NotNull @DecimalMin(value = "0.00000001") BigDecimal quantity,
    @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal averageCost,
    @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal currentPrice,
    @Pattern(regexp = "^[A-Z]{3}$") String currency
) {}
