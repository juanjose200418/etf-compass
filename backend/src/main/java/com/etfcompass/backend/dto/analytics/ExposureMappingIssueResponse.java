package com.etfcompass.backend.dto.analytics;

import java.math.BigDecimal;

public record ExposureMappingIssueResponse(
    String ticker,
    String name,
    BigDecimal portfolioPercentage,
    BigDecimal unclassifiedPortfolioPercentage,
    BigDecimal holdingCoveragePercentage,
    String sourceType,
    String reason
) {}
