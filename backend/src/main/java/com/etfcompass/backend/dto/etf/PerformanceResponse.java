package com.etfcompass.backend.dto.etf;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public record PerformanceResponse(
    @JsonProperty("1Y")
    BigDecimal oneYear,
    @JsonProperty("3Y")
    BigDecimal threeYear,
    @JsonProperty("5Y")
    BigDecimal fiveYear
) {}
