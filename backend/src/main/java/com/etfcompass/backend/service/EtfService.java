package com.etfcompass.backend.service;

import com.etfcompass.backend.domain.DistributionPolicy;
import com.etfcompass.backend.domain.Etf;
import com.etfcompass.backend.domain.EtfHolding;
import com.etfcompass.backend.dto.etf.EtfDetailResponse;
import com.etfcompass.backend.dto.etf.EtfCompareResponse;
import com.etfcompass.backend.dto.etf.EtfOverlapResponse;
import com.etfcompass.backend.dto.etf.EtfResponse;
import com.etfcompass.backend.dto.etf.SharedHoldingResponse;
import com.etfcompass.backend.exception.NotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import com.etfcompass.backend.repository.EtfExposureRepository;
import com.etfcompass.backend.repository.EtfHoldingRepository;
import com.etfcompass.backend.repository.EtfRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EtfService {

  private final EtfRepository etfRepository;
  private final EtfExposureRepository exposureRepository;
  private final EtfHoldingRepository holdingRepository;
  private final EtfMapper mapper;
  private final MarketDataEnrichmentService marketDataEnrichmentService;
  private final EtfComparisonService etfComparisonService;

  @Transactional(readOnly = true)
  public List<EtfResponse> search(String query) {
    String q = query == null ? "" : query.trim();
    return etfRepository.findTop20ByTickerContainingIgnoreCaseOrNameContainingIgnoreCaseOrderByTickerAsc(q, q)
        .stream().map(mapper::toResponse).toList();
  }

  @Transactional
  public EtfDetailResponse getDetail(String ticker) {
    String normalized = resolveKnownTicker(ticker == null ? "" : ticker.trim());
    var live = etfComparisonService.getEtf(normalized);
    var existing = etfRepository.findByTickerIgnoreCase(normalized);
    var exposures = existing.map(etf -> exposureRepository.findByEtf_Id(etf.getId()).stream().map(mapper::toExposureResponse).toList()).orElse(List.of());
    var holdings = existing.map(etf -> holdingRepository.findByEtf_IdOrderByWeightDesc(etf.getId()).stream().map(mapper::toHoldingResponse).toList()).orElse(List.of());
    return new EtfDetailResponse(live.etf(), exposures, holdings, live.warnings());
  }

  @Transactional
  public EtfCompareResponse compare(List<String> tickers) {
    return etfComparisonService.compare(
        tickers.stream().distinct().map(ticker -> resolveKnownTicker(ticker == null ? "" : ticker.trim())).toList()
    );
  }

  @Transactional
  public EtfOverlapResponse overlap(String leftTicker, String rightTicker) {
    Etf left = ensureForRead(leftTicker);
    Etf right = ensureForRead(rightTicker);

    Map<String, EtfHolding> leftHoldings = bySymbol(holdingRepository.findByEtf_IdOrderByWeightDesc(left.getId()));
    Map<String, EtfHolding> rightHoldings = bySymbol(holdingRepository.findByEtf_IdOrderByWeightDesc(right.getId()));

    List<SharedHoldingResponse> shared = leftHoldings.entrySet().stream()
        .filter(entry -> rightHoldings.containsKey(entry.getKey()))
        .map(entry -> {
          EtfHolding leftHolding = entry.getValue();
          EtfHolding rightHolding = rightHoldings.get(entry.getKey());
          BigDecimal overlapWeight = leftHolding.getWeight().min(rightHolding.getWeight()).setScale(4, RoundingMode.HALF_UP);
          return new SharedHoldingResponse(
              entry.getKey(),
              leftHolding.getName() == null || leftHolding.getName().isBlank() ? rightHolding.getName() : leftHolding.getName(),
              leftHolding.getWeight(),
              rightHolding.getWeight(),
              overlapWeight);
        })
        .sorted((a, b) -> b.overlapWeight().compareTo(a.overlapWeight()))
        .toList();

    BigDecimal totalOverlap = shared.stream()
        .map(SharedHoldingResponse::overlapWeight)
        .reduce(BigDecimal.ZERO, BigDecimal::add)
        .setScale(4, RoundingMode.HALF_UP);

    return new EtfOverlapResponse(left.getTicker(), right.getTicker(), totalOverlap, shared);
  }

  @Transactional
  public Etf getOrCreateMinimal(String tickerOrName, String name, String currency) {
    String query = tickerOrName.trim();
    String normalized = resolveKnownTicker(query);
    Etf etf = etfRepository.findByTickerIgnoreCase(query)
        .or(() -> etfRepository.findByTickerIgnoreCase(normalized))
        .or(() -> etfRepository.findTop20ByTickerContainingIgnoreCaseOrNameContainingIgnoreCaseOrderByTickerAsc(query, query).stream().findFirst())
        .orElseGet(() -> {
          var created = new Etf(normalized, name == null || name.isBlank() ? query : name.trim());
          created.setCurrency(currency == null || currency.isBlank() ? "EUR" : currency);
          created.setProvider("Imported");
          created.setDistributionPolicy(DistributionPolicy.ACCUMULATING);
          return etfRepository.save(created);
        });
    marketDataEnrichmentService.enrichIfConfigured(etf);
    return etf;
  }

  private Etf ensureForRead(String ticker) {
    String normalized = resolveKnownTicker(ticker == null ? "" : ticker.trim());
    Etf etf = etfRepository.findByTickerIgnoreCase(normalized)
        .orElseGet(() -> etfRepository.save(new Etf(normalized, normalized)));

    marketDataEnrichmentService.refreshIfConfigured(etf);

    return etfRepository.findByTickerIgnoreCase(etf.getTicker()).orElse(etf);
  }

  private String normalizeTicker(String value) {
    String normalized = value.toUpperCase().replaceAll("[^A-Z0-9.]", "");
    if (normalized.isBlank()) {
      return "UNKNOWN";
    }
    return normalized.length() > 32 ? normalized.substring(0, 32) : normalized;
  }

  private String resolveKnownTicker(String value) {
    String normalizedText = value.toUpperCase()
        .replace("&", "AND")
        .replaceAll("[^A-Z0-9]+", " ")
        .trim();

    if (normalizedText.contains("MSCI ACWI") || normalizedText.equals("IUSQ")) return "IUSQ";
    if (normalizedText.contains("EM ASIA") || normalizedText.contains("EMERGING MARKETS ASIA")) return "EEMA";
    if (normalizedText.contains("FTSE DEVELOPED") || normalizedText.equals("VGVF")) return "VGVF";
    if (normalizedText.contains("DIVERSIFIED COMMODITIES") || normalizedText.contains("COMMODITIES")) return "ICOM";
    if (normalizedText.contains("DIVIDEND ARISTOCRATS") || normalizedText.equals("SPYD")) return "SPYD";
    if (normalizedText.contains("WORLD INFORMATION TECHNOLOGY") || normalizedText.contains("WORLD IT")) return "XDWT";
    if (normalizedText.contains("WORLD SMALL CAP") || normalizedText.equals("IUSN")) return "IUSN";
    if (normalizedText.contains("URANIUM") || normalizedText.contains("NUCLEAR") || normalizedText.equals("NUKL")) return "NUKL";
    if (normalizedText.contains("CHINA TECH") || normalizedText.contains("CHINA TECHNOLOGY")) return "CQQQ";
    return normalizeTicker(value);
  }

  public Etf findByTicker(String ticker) {
    return etfRepository.findByTickerIgnoreCase(ticker)
        .orElseThrow(() -> new NotFoundException("ETF not found: " + ticker));
  }

  private Map<String, EtfHolding> bySymbol(List<EtfHolding> holdings) {
    Map<String, EtfHolding> bySymbol = new LinkedHashMap<>();
    for (EtfHolding holding : holdings) {
      String symbol = holding.getSymbol() == null ? "" : holding.getSymbol().trim().toUpperCase(Locale.ROOT);
      if (!symbol.isBlank()) {
        bySymbol.put(symbol, holding);
      }
    }
    return bySymbol;
  }
}
