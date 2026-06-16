package com.etfcompass.backend.dto.etf;

import java.math.BigDecimal;

public record SharedHoldingResponse(
    String symbol,
    String name,
    BigDecimal leftWeight,
    BigDecimal rightWeight,
    BigDecimal overlapWeight
) {}
