package com.etfcompass.backend.dto.analytics;

import com.etfcompass.backend.dto.portfolio.PositionResponse;
import java.math.BigDecimal;
import java.util.List;

public record PortfolioAnalyticsResponse(
    BigDecimal totalPortfolioValue,
    BigDecimal totalInvestedCapital,
    BigDecimal totalProfitLoss,
    BigDecimal profitLossPercentage,
    List<AllocationSliceResponse> portfolioAllocation,
    List<AllocationSliceResponse> etfAllocation,
    List<AllocationSliceResponse> countryExposure,
    List<AllocationSliceResponse> sectorExposure,
    List<AllocationSliceResponse> industryExposure,
    List<AllocationSliceResponse> currencyExposure,
    ExposureDataQualityResponse countryDataQuality,
    ExposureDataQualityResponse sectorDataQuality,
    ExposureDataQualityResponse industryDataQuality,
    List<HoldingExposureResponse> topHoldingsExposure,
    List<PositionResponse> bestPerformingPositions,
    List<PositionResponse> worstPerformingPositions
) {}
