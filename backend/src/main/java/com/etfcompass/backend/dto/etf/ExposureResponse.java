package com.etfcompass.backend.dto.etf;

import com.etfcompass.backend.domain.ExposureType;
import java.math.BigDecimal;

public record ExposureResponse(
    ExposureType type,
    String name,
    BigDecimal weight
) {}
