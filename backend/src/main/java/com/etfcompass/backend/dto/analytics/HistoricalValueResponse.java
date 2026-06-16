package com.etfcompass.backend.dto.analytics;

import java.math.BigDecimal;
import java.time.LocalDate;

public record HistoricalValueResponse(
    LocalDate date,
    BigDecimal value
) {}
