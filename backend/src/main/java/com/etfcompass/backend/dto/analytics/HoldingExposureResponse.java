package com.etfcompass.backend.dto.analytics;

import java.math.BigDecimal;

public record HoldingExposureResponse(
    String symbol,
    String name,
    BigDecimal value,
    BigDecimal percentage
) {}
