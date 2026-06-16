package com.etfcompass.backend.dto.etf;

import java.math.BigDecimal;
import java.util.List;

public record EtfOverlapResponse(
    String leftTicker,
    String rightTicker,
    BigDecimal overlapPercentage,
    List<SharedHoldingResponse> sharedHoldings
) {}
