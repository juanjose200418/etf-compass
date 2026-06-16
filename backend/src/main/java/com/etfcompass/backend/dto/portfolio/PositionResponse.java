package com.etfcompass.backend.dto.portfolio;

import com.etfcompass.backend.domain.Broker;
import java.math.BigDecimal;
import java.util.UUID;

public record PositionResponse(
    UUID id,
    String ticker,
    String name,
    Broker broker,
    BigDecimal quantity,
    BigDecimal averageCost,
    BigDecimal currentPrice,
    BigDecimal investedCapital,
    BigDecimal currentValue,
    BigDecimal profitLoss,
    BigDecimal profitLossPercentage,
    String currency
) {}
