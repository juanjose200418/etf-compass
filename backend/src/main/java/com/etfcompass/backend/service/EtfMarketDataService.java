package com.etfcompass.backend.service;

import com.etfcompass.backend.config.MarketDataProperties;
import com.etfcompass.backend.dto.etf.EtfResponse;
import com.etfcompass.backend.dto.etf.PerformanceResponse;
import com.etfcompass.backend.dto.etf.QuoteSnapshotResponse;
import com.etfcompass.backend.exception.BadRequestException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EtfMarketDataService {

  private final MarketDataProperties properties;
  private final FinnhubMarketDataProvider finnhubProvider;
  private final FmpMarketDataProvider fmpProvider;

  public MarketDataResult getEtf(String ticker) {
    return switch (properties.normalizedProvider()) {
      case "finnhub" -> withOptionalFallback(finnhubProvider, fmpProvider, ticker);
      case "fmp" -> withOptionalFallback(fmpProvider, finnhubProvider, ticker);
      default -> throw new BadRequestException("Unsupported market data provider: " + properties.normalizedProvider());
    };
  }

  private MarketDataResult withOptionalFallback(MarketDataProvider primary, MarketDataProvider secondary, String ticker) {
    MarketDataResult primaryResult = primary.fetchEtf(ticker);
    List<String> warnings = new ArrayList<>(primaryResult.warnings());
    EtfResponse merged = primaryResult.etf();

    if (secondary.isConfigured()) {
      try {
        MarketDataResult secondaryResult = secondary.fetchEtf(ticker);
        merged = merge(merged, secondaryResult.etf());
        warnings.addAll(secondaryResult.warnings());
      } catch (BadRequestException ex) {
        warnings.add(ex.getMessage());
      }
    }

    return new MarketDataResult(merged, List.copyOf(new LinkedHashSet<>(warnings)));
  }

  private EtfResponse merge(EtfResponse preferred, EtfResponse fallback) {
    if (preferred == null) return fallback;
    if (fallback == null) return preferred;

    PerformanceResponse preferredPerformance = preferred.performance();
    PerformanceResponse fallbackPerformance = fallback.performance();

    return new EtfResponse(
        preferred.id() != null ? preferred.id() : fallback.id(),
        preferred.ticker() != null ? preferred.ticker() : fallback.ticker(),
        firstNonBlank(preferred.name(), fallback.name()),
        firstNonBlank(preferred.isin(), fallback.isin()),
        firstNonBlank(preferred.provider(), fallback.provider()),
        preferred.ter() != null ? preferred.ter() : fallback.ter(),
        firstNonBlank(preferred.assetClass(), fallback.assetClass()),
        firstNonBlank(preferred.region(), fallback.region()),
        firstNonBlank(preferred.indexTracked(), fallback.indexTracked()),
        preferred.distributionPolicy() != null ? preferred.distributionPolicy() : fallback.distributionPolicy(),
        preferred.fundSize() != null ? preferred.fundSize() : fallback.fundSize(),
        firstNonBlank(preferred.currency(), fallback.currency()),
        preferred.riskLevel() != null ? preferred.riskLevel() : fallback.riskLevel(),
        firstNonBlank(preferred.metadataSource(), fallback.metadataSource()),
        preferred.lookThroughCoverage() != null ? preferred.lookThroughCoverage() : fallback.lookThroughCoverage(),
        preferred.metadataUpdatedAt() != null ? preferred.metadataUpdatedAt() : fallback.metadataUpdatedAt(),
        new PerformanceResponse(
            preferredPerformance != null && preferredPerformance.oneYear() != null ? preferredPerformance.oneYear() : fallbackPerformance != null ? fallbackPerformance.oneYear() : null,
            preferredPerformance != null && preferredPerformance.threeYear() != null ? preferredPerformance.threeYear() : fallbackPerformance != null ? fallbackPerformance.threeYear() : null,
            preferredPerformance != null && preferredPerformance.fiveYear() != null ? preferredPerformance.fiveYear() : fallbackPerformance != null ? fallbackPerformance.fiveYear() : null
        ),
        new QuoteSnapshotResponse(null, null, null, null, null, null, null)
    );
  }

  private String firstNonBlank(String preferred, String fallback) {
    if (preferred != null && !preferred.isBlank()) return preferred;
    return fallback != null && !fallback.isBlank() ? fallback : null;
  }
}
