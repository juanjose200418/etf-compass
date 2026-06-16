package com.etfcompass.backend.dto.dashboard;

import com.etfcompass.backend.dto.analytics.AllocationSliceResponse;
import com.etfcompass.backend.dto.analytics.HistoricalValueResponse;
import com.etfcompass.backend.dto.portfolio.PositionResponse;
import java.math.BigDecimal;
import java.util.List;

public record DashboardResponse(
    BigDecimal netWorth,
    BigDecimal totalInvestedCapital,
    BigDecimal totalProfitLoss,
    BigDecimal profitLossPercentage,
    int portfolioCount,
    int positionCount,
    List<AllocationSliceResponse> assetAllocation,
    List<AllocationSliceResponse> etfAllocation,
    List<AllocationSliceResponse> industryAllocation,
    List<AllocationSliceResponse> sectorAllocation,
    List<AllocationSliceResponse> geographicAllocation,
    List<AllocationSliceResponse> currencyAllocation,
    List<HistoricalValueResponse> historicalPortfolioEvolution,
    List<PositionResponse> bestPerformingPositions,
    List<PositionResponse> worstPerformingPositions
) {}
