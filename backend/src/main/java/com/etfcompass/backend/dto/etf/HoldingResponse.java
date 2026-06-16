package com.etfcompass.backend.dto.etf;

import java.math.BigDecimal;

public record HoldingResponse(
    String symbol,
    String name,
    String country,
    String sector,
    String industry,
    BigDecimal weight
) {}
