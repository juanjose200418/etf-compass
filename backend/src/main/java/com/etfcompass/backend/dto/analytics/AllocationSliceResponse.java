package com.etfcompass.backend.dto.analytics;

import java.math.BigDecimal;

public record AllocationSliceResponse(
    String label,
    BigDecimal value,
    BigDecimal percentage
) {}
