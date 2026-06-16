package com.etfcompass.backend.dto.portfolio;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record PortfolioResponse(
    UUID id,
    String name,
    String baseCurrency,
    BigDecimal totalValue,
    BigDecimal totalInvested,
    List<PositionResponse> positions
) {}
