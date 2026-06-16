package com.etfcompass.backend.dto.etf;

import com.etfcompass.backend.domain.DistributionPolicy;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record EtfResponse(
    UUID id,
    String ticker,
    String name,
    String isin,
    String provider,
    BigDecimal ter,
    String assetClass,
    String region,
    String indexTracked,
    DistributionPolicy distributionPolicy,
    BigDecimal fundSize,
    String currency,
    Integer riskLevel,
    String metadataSource,
    BigDecimal lookThroughCoverage,
    LocalDate metadataUpdatedAt,
    PerformanceResponse performance,
    QuoteSnapshotResponse quote
) {}
