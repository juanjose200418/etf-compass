package com.etfcompass.backend.dto.etf;

import java.math.BigDecimal;

public record QuoteSnapshotResponse(
    BigDecimal currentPrice,
    BigDecimal change,
    BigDecimal changePercent,
    BigDecimal dayHigh,
    BigDecimal dayLow,
    BigDecimal open,
    BigDecimal previousClose
) {}
