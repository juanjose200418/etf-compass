package com.etfcompass.backend.service;

import com.etfcompass.backend.config.MarketDataProperties;
import com.etfcompass.backend.domain.Etf;
import com.etfcompass.backend.dto.etf.EtfCompareResponse;
import com.etfcompass.backend.dto.etf.EtfResponse;
import com.etfcompass.backend.dto.etf.PerformanceResponse;
import com.etfcompass.backend.dto.etf.QuoteSnapshotResponse;
import com.etfcompass.backend.exception.BadRequestException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EtfComparisonService {

  private final MarketDataProperties properties;
  private final EtfMetadataRepository metadataRepository;
  private final EtfMapper mapper;
  private final EtfMarketDataService etfMarketDataService;
  private final FinnhubQuoteProvider finnhubQuoteProvider;
  private final FmpQuoteProvider fmpQuoteProvider;

  public MarketDataResult getEtf(String ticker) {
    String normalized = normalizeTicker(ticker);
    List<String> warnings = new ArrayList<>();
    Optional<Etf> metadata = metadataRepository.findByTicker(normalized);
    if (metadata.isEmpty()) {
      warnings.add("Dato no disponible en metadata local para " + normalized + ".");
    }

    MarketDataResult marketData = fetchMarketData(normalized);
    warnings.addAll(marketData.warnings());

    QuoteResult quote = fetchQuote(normalized);
    warnings.addAll(quote.warnings());

    EtfResponse metadataResponse = mergeEtfData(metadata.map(this::mapMetadata).orElseGet(() -> emptyMetadata(normalized)), marketData.etf());
    EtfResponse combined = new EtfResponse(
        metadataResponse.id(),
        metadataResponse.ticker(),
        metadataResponse.name(),
        metadataResponse.isin(),
        metadataResponse.provider(),
        metadataResponse.ter(),
        metadataResponse.assetClass(),
        metadataResponse.region(),
        metadataResponse.indexTracked(),
        metadataResponse.distributionPolicy(),
        metadataResponse.fundSize(),
        metadataResponse.currency(),
        metadataResponse.riskLevel(),
        metadataResponse.metadataSource(),
        metadataResponse.lookThroughCoverage(),
        metadataResponse.metadataUpdatedAt(),
        metadataResponse.performance(),
        quote.quote()
    );

    return new MarketDataResult(combined, List.copyOf(new LinkedHashSet<>(warnings)));
  }

  public EtfCompareResponse compare(List<String> tickers) {
    List<EtfResponse> etfs = new ArrayList<>();
    List<String> warnings = new ArrayList<>();

    tickers.stream().distinct().map(this::getEtf).forEach(result -> {
      etfs.add(result.etf());
      warnings.addAll(result.warnings());
    });

    return new EtfCompareResponse(etfs, List.copyOf(new LinkedHashSet<>(warnings)));
  }

  private QuoteResult fetchQuote(String ticker) {
    MarketQuoteProvider primary = properties.usesFmp() ? fmpQuoteProvider : finnhubQuoteProvider;
    MarketQuoteProvider fallback = properties.usesFmp() ? finnhubQuoteProvider : fmpQuoteProvider;

    try {
      return primary.fetchQuote(ticker);
    } catch (BadRequestException primaryError) {
      if (fallback.isConfigured()) {
        try {
          QuoteResult fallbackResult = fallback.fetchQuote(ticker);
          List<String> warnings = new ArrayList<>();
          warnings.add(primaryError.getMessage());
          warnings.addAll(fallbackResult.warnings());
          return new QuoteResult(fallbackResult.quote(), List.copyOf(new LinkedHashSet<>(warnings)));
        } catch (BadRequestException fallbackError) {
          return new QuoteResult(new QuoteSnapshotResponse(null, null, null, null, null, null, null), List.of(primaryError.getMessage(), fallbackError.getMessage()));
        }
      }
      return new QuoteResult(new QuoteSnapshotResponse(null, null, null, null, null, null, null), List.of(primaryError.getMessage()));
    }
  }

  private MarketDataResult fetchMarketData(String ticker) {
    try {
      return etfMarketDataService.getEtf(ticker);
    } catch (BadRequestException ex) {
      return new MarketDataResult(emptyMetadata(ticker), List.of(ex.getMessage()));
    }
  }

  private EtfResponse mergeEtfData(EtfResponse preferred, EtfResponse fallback) {
    if (preferred == null) return fallback;
    if (fallback == null) return preferred;

    PerformanceResponse preferredPerformance = preferred.performance();
    PerformanceResponse fallbackPerformance = fallback.performance();

    return new EtfResponse(
        preferred.id() != null ? preferred.id() : fallback.id(),
        firstNonBlank(preferred.ticker(), fallback.ticker()),
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

  private EtfResponse mapMetadata(Etf etf) {
    EtfResponse base = mapper.toResponse(etf);
    return new EtfResponse(
        base.id(),
        base.ticker(),
        base.name(),
        base.isin(),
        base.provider(),
        base.ter(),
        base.assetClass(),
        base.region(),
        base.indexTracked(),
        base.distributionPolicy(),
        base.fundSize(),
        base.currency(),
        base.riskLevel(),
        base.metadataSource(),
        base.lookThroughCoverage(),
        base.metadataUpdatedAt(),
        new PerformanceResponse(null, null, null),
        new QuoteSnapshotResponse(null, null, null, null, null, null, null)
    );
  }

  private EtfResponse emptyMetadata(String ticker) {
    return new EtfResponse(null, ticker, ticker, null, null, null, null, null, null, null, null, null, null, null, null, null, new PerformanceResponse(null, null, null), new QuoteSnapshotResponse(null, null, null, null, null, null, null));
  }

  private String normalizeTicker(String ticker) {
    String normalized = ticker == null ? "" : ticker.trim().toUpperCase().replaceAll("[^A-Z0-9.]", "");
    if (normalized.isBlank()) {
      throw new BadRequestException("Ticker is required");
    }
    return normalized;
  }

  private String firstNonBlank(String preferred, String fallback) {
    if (preferred != null && !preferred.isBlank()) return preferred;
    return fallback != null && !fallback.isBlank() ? fallback : null;
  }
}
