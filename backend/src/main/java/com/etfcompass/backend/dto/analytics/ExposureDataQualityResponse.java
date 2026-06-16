package com.etfcompass.backend.dto.analytics;

import java.math.BigDecimal;
import java.util.List;

public record ExposureDataQualityResponse(
    BigDecimal coveragePercentage,
    BigDecimal unclassifiedPercentage,
    BigDecimal lookThroughCoveragePercentage,
    BigDecimal estimatedPercentage,
    List<ExposureMappingIssueResponse> affectedEtfs
) {}
