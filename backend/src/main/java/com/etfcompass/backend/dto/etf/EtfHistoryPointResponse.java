package com.etfcompass.backend.dto.etf;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EtfHistoryPointResponse(
    LocalDate date,
    BigDecimal close
) {}
