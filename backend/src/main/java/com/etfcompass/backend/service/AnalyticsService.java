package com.etfcompass.backend.service;

import com.etfcompass.backend.domain.EtfHolding;
import com.etfcompass.backend.domain.ExposureType;
import com.etfcompass.backend.domain.Position;
import com.etfcompass.backend.dto.analytics.AllocationSliceResponse;
import com.etfcompass.backend.dto.analytics.ExposureDataQualityResponse;
import com.etfcompass.backend.dto.analytics.ExposureMappingIssueResponse;
import com.etfcompass.backend.dto.analytics.HistoricalValueResponse;
import com.etfcompass.backend.dto.analytics.HoldingExposureResponse;
import com.etfcompass.backend.dto.analytics.PortfolioAnalyticsResponse;
import com.etfcompass.backend.dto.dashboard.DashboardResponse;
import com.etfcompass.backend.dto.portfolio.PositionResponse;
import com.etfcompass.backend.repository.EtfExposureRepository;
import com.etfcompass.backend.repository.EtfHoldingRepository;
import com.etfcompass.backend.repository.PortfolioRepository;
import com.etfcompass.backend.repository.PositionRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

  private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
  private static final BigDecimal MIN_HOLDING_BREAKDOWN_WEIGHT = BigDecimal.valueOf(35);

  private final PortfolioService portfolioService;
  private final PositionRepository positionRepository;
  private final PortfolioRepository portfolioRepository;
  private final EtfExposureRepository exposureRepository;
  private final EtfHoldingRepository holdingRepository;
  private final PositionMapper positionMapper;

  @Transactional(readOnly = true)
  public PortfolioAnalyticsResponse portfolioAnalytics(String email, UUID portfolioId) {
    portfolioService.findOwned(email, portfolioId);
    List<Position> positions = positionRepository.findByPortfolio_IdAndPortfolio_User_EmailIgnoreCase(portfolioId, email);
    return buildPortfolioAnalytics(positions);
  }

  @Transactional(readOnly = true)
  public DashboardResponse dashboard(String email) {
    var portfolios = portfolioRepository.findByUser_EmailIgnoreCaseOrderByCreatedAtDesc(email);
    List<Position> positions = portfolios.stream()
        .flatMap(portfolio -> positionRepository.findByPortfolio_IdOrderByCreatedAtAsc(portfolio.getId()).stream())
        .toList();
    var analytics = buildPortfolioAnalytics(positions);
    return new DashboardResponse(
        analytics.totalPortfolioValue(),
        analytics.totalInvestedCapital(),
        analytics.totalProfitLoss(),
        analytics.profitLossPercentage(),
        portfolios.size(),
        positions.size(),
        analytics.portfolioAllocation(),
        analytics.etfAllocation(),
        analytics.industryExposure(),
        analytics.sectorExposure(),
        analytics.countryExposure(),
        analytics.currencyExposure(),
        List.of(new HistoricalValueResponse(LocalDate.now(), analytics.totalPortfolioValue())),
        analytics.bestPerformingPositions(),
        analytics.worstPerformingPositions()
    );
  }

  private PortfolioAnalyticsResponse buildPortfolioAnalytics(List<Position> positions) {
    List<PositionResponse> positionResponses = positions.stream().map(positionMapper::toResponse).toList();
    BigDecimal totalValue = positionResponses.stream().map(PositionResponse::currentValue).reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal totalInvested = positionResponses.stream().map(PositionResponse::investedCapital).reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal profitLoss = totalValue.subtract(totalInvested);
    BigDecimal profitLossPct = percentage(profitLoss, totalInvested);
    ExposureAnalysis countryAnalysis = exposureAnalysis(
        positions,
        totalValue,
        ExposureType.COUNTRY,
        position -> safe(position.getEtf().getRegion(), unclassifiedLabel(ExposureType.COUNTRY)),
        EtfHolding::getCountry);
    ExposureAnalysis sectorAnalysis = exposureAnalysis(
        positions,
        totalValue,
        ExposureType.SECTOR,
        position -> safe(position.getEtf().getAssetClass(), unclassifiedLabel(ExposureType.SECTOR)),
        EtfHolding::getSector);
    ExposureAnalysis industryAnalysis = exposureAnalysis(
        positions,
        totalValue,
        ExposureType.INDUSTRY,
        position -> safe(position.getEtf().getAssetClass(), unclassifiedLabel(ExposureType.INDUSTRY)),
        EtfHolding::getIndustry);

    return new PortfolioAnalyticsResponse(
        totalValue,
        totalInvested,
        profitLoss,
        profitLossPct,
        exposureByFallback(positions, totalValue, ExposureType.ASSET_CLASS, position -> safe(position.getEtf().getAssetClass(), "ETF")),
        allocationByPosition(positionResponses, totalValue),
        countryAnalysis.slices(),
        sectorAnalysis.slices(),
        industryAnalysis.slices(),
        exposureByFallback(positions, totalValue, ExposureType.CURRENCY, position -> safe(position.getCurrency(), unclassifiedLabel(ExposureType.CURRENCY))),
        countryAnalysis.dataQuality(),
        sectorAnalysis.dataQuality(),
        industryAnalysis.dataQuality(),
        topHoldings(positions, totalValue),
        positionResponses.stream().sorted(Comparator.comparing(PositionResponse::profitLossPercentage).reversed()).limit(5).toList(),
        positionResponses.stream().sorted(Comparator.comparing(PositionResponse::profitLossPercentage)).limit(5).toList()
    );
  }

  private List<AllocationSliceResponse> allocationByPosition(List<PositionResponse> positions, BigDecimal totalValue) {
    return positions.stream()
        .sorted(Comparator.comparing(PositionResponse::currentValue).reversed())
        .map(position -> new AllocationSliceResponse(positionLabel(position), position.currentValue(), percentage(position.currentValue(), totalValue)))
        .toList();
  }

  private List<AllocationSliceResponse> exposureByHoldingsOrFallback(
      List<Position> positions,
      BigDecimal totalValue,
      ExposureType type,
      java.util.function.Function<Position, String> fallback,
      java.util.function.Function<EtfHolding, String> holdingLabelExtractor
  ) {
    Map<String, BigDecimal> amounts = new HashMap<>();
    for (Position position : positions) {
      BigDecimal value = position.getQuantity().multiply(position.getCurrentPrice());
      ExposureResolution resolution = resolvePositionExposure(position, type, fallback, holdingLabelExtractor);
      for (var entry : resolution.weights().entrySet()) {
        amounts.merge(entry.getKey(), weighted(value, entry.getValue()), BigDecimal::add);
      }
    }
    return toSlices(amounts, totalValue);
  }

  private ExposureAnalysis exposureAnalysis(
      List<Position> positions,
      BigDecimal totalValue,
      ExposureType type,
      java.util.function.Function<Position, String> fallback,
      java.util.function.Function<EtfHolding, String> holdingLabelExtractor
  ) {
    Map<String, BigDecimal> amounts = new HashMap<>();
    List<ExposureMappingIssueResponse> issues = new ArrayList<>();
    BigDecimal lookThroughValue = BigDecimal.ZERO;

    for (Position position : positions) {
      BigDecimal positionValue = position.getQuantity().multiply(position.getCurrentPrice());
      ExposureResolution resolution = resolvePositionExposure(position, type, fallback, holdingLabelExtractor);

      for (var entry : resolution.weights().entrySet()) {
        amounts.merge(entry.getKey(), weighted(positionValue, entry.getValue()), BigDecimal::add);
      }

      lookThroughValue = lookThroughValue.add(weighted(positionValue, resolution.lookThroughCoveragePercentage()));

      BigDecimal unclassifiedWeight = resolution.weights().entrySet().stream()
          .filter(entry -> isUnclassifiedLabel(entry.getKey()))
          .map(Map.Entry::getValue)
          .reduce(BigDecimal.ZERO, BigDecimal::add);

      if (resolution.warningReason() != null) {
        issues.add(new ExposureMappingIssueResponse(
            position.getEtf().getTicker(),
            position.getEtf().getName(),
            percentage(positionValue, totalValue),
            percentage(weighted(positionValue, unclassifiedWeight), totalValue),
            resolution.holdingCoveragePercentage(),
            resolution.sourceType(),
            resolution.warningReason()));
      }
    }

    List<AllocationSliceResponse> slices = toSlices(amounts, totalValue);
    BigDecimal unclassified = slices.stream()
        .filter(slice -> isUnclassifiedLabel(slice.label()))
        .map(AllocationSliceResponse::percentage)
        .reduce(BigDecimal.ZERO, BigDecimal::add);

    ExposureDataQualityResponse dataQuality = new ExposureDataQualityResponse(
        HUNDRED.subtract(unclassified).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP),
        unclassified.setScale(2, RoundingMode.HALF_UP),
        percentage(lookThroughValue, totalValue).setScale(2, RoundingMode.HALF_UP),
        HUNDRED.subtract(unclassified).subtract(percentage(lookThroughValue, totalValue)).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP),
        issues.stream().sorted(Comparator.comparing(ExposureMappingIssueResponse::portfolioPercentage).reversed()).toList());

    return new ExposureAnalysis(slices, dataQuality);
  }

  private List<AllocationSliceResponse> exposureByFallback(
      List<Position> positions,
      BigDecimal totalValue,
      ExposureType type,
      java.util.function.Function<Position, String> fallback
  ) {
    Map<String, BigDecimal> amounts = new HashMap<>();
    for (Position position : positions) {
      BigDecimal value = position.getQuantity().multiply(position.getCurrentPrice());
      var exposures = exposureRepository.findByEtf_IdAndType(position.getEtf().getId(), type);
      if (exposures.isEmpty()) {
        amounts.merge(fallback.apply(position), value, BigDecimal::add);
        continue;
      }
      for (var exposure : exposures) {
        amounts.merge(sanitizeExposureLabel(type, exposure.getName()), weighted(value, exposure.getWeight()), BigDecimal::add);
      }
    }
    return toSlices(amounts, totalValue);
  }

  private List<HoldingExposureResponse> topHoldings(List<Position> positions, BigDecimal totalValue) {
    Map<String, HoldingAmount> amounts = new HashMap<>();
    for (Position position : positions) {
      BigDecimal value = position.getQuantity().multiply(position.getCurrentPrice());
      for (var holding : holdingRepository.findByEtf_IdOrderByWeightDesc(position.getEtf().getId())) {
        BigDecimal weightedValue = weighted(value, holding.getWeight());
        amounts.merge(
            holding.getSymbol(),
            new HoldingAmount(holding.getName(), weightedValue),
            (left, right) -> new HoldingAmount(left.name(), left.value().add(right.value())));
      }
    }
    return amounts.entrySet().stream()
        .sorted(Map.Entry.<String, HoldingAmount>comparingByValue(Comparator.comparing(HoldingAmount::value)).reversed())
        .limit(10)
        .map(entry -> new HoldingExposureResponse(entry.getKey(), entry.getValue().name(), entry.getValue().value(), percentage(entry.getValue().value(), totalValue)))
        .toList();
  }

  private List<AllocationSliceResponse> toSlices(Map<String, BigDecimal> amounts, BigDecimal totalValue) {
    return amounts.entrySet().stream()
        .filter(entry -> entry.getValue() != null && entry.getValue().signum() > 0)
        .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
        .map(entry -> new AllocationSliceResponse(entry.getKey(), entry.getValue(), percentage(entry.getValue(), totalValue)))
        .toList();
  }

  private ExposureResolution resolvePositionExposure(
      Position position,
      ExposureType type,
      java.util.function.Function<Position, String> fallback,
      java.util.function.Function<EtfHolding, String> holdingLabelExtractor
  ) {
    List<EtfHolding> holdings = holdingRepository.findByEtf_IdOrderByWeightDesc(position.getEtf().getId());
    Map<String, BigDecimal> holdingWeights = aggregateHoldingWeights(holdings, holdingLabelExtractor);
    BigDecimal holdingCoverage = holdingWeights.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);

    var exposures = exposureRepository.findByEtf_IdAndType(position.getEtf().getId(), type);
    Map<String, BigDecimal> exposureWeights = aggregateExposureWeights(type, exposures);

    if (!holdingWeights.isEmpty() && !exposureWeights.isEmpty()) {
      return mergeHoldingAndExposureWeights(position, type, holdingWeights, holdingCoverage, exposureWeights);
    }

    if (!holdingWeights.isEmpty()) {
      Map<String, BigDecimal> weights = new LinkedHashMap<>(holdingWeights);
      BigDecimal remaining = HUNDRED.subtract(holdingCoverage).max(BigDecimal.ZERO);
      if (remaining.signum() > 0) {
        weights.merge(unclassifiedLabel(type), remaining, BigDecimal::add);
      }
      BigDecimal lookThroughCoverage = resolveLookThroughCoverage(position, holdingCoverage, !weights.isEmpty());
      String warning = holdingCoverage.compareTo(HUNDRED) < 0
          ? "Only " + displayPercent(holdingCoverage) + "% of holdings had " + type.name().toLowerCase() + " metadata and no ETF-level fallback data was available."
          : null;
      return new ExposureResolution(weights, holdingCoverage, lookThroughCoverage, resolveSourceType(position, lookThroughCoverage, holdingCoverage), warning);
    }

    if (!exposureWeights.isEmpty()) {
      BigDecimal lookThroughCoverage = resolveLookThroughCoverage(position, BigDecimal.ZERO, true);
      return new ExposureResolution(
          exposureWeights,
          BigDecimal.ZERO,
          lookThroughCoverage,
          resolveSourceType(position, lookThroughCoverage, BigDecimal.ZERO),
          "This ETF used provider-level " + type.name().toLowerCase() + " exposure because holdings metadata was unavailable.");
    }

    return new ExposureResolution(Map.of(fallback.apply(position), HUNDRED), BigDecimal.ZERO, BigDecimal.ZERO, "fallback", "No holdings or provider metadata was available for this ETF.");
  }

  private ExposureResolution mergeHoldingAndExposureWeights(
      Position position,
      ExposureType type,
      Map<String, BigDecimal> holdingWeights,
      BigDecimal holdingCoverage,
      Map<String, BigDecimal> exposureWeights
  ) {
    Map<String, BigDecimal> merged = new LinkedHashMap<>(holdingWeights);
    BigDecimal remaining = HUNDRED.subtract(holdingCoverage).max(BigDecimal.ZERO);

    if (remaining.signum() > 0) {
      Map<String, BigDecimal> residualTemplate = new LinkedHashMap<>();
      for (var entry : exposureWeights.entrySet()) {
        BigDecimal covered = holdingWeights.getOrDefault(entry.getKey(), BigDecimal.ZERO);
        BigDecimal residual = entry.getValue().subtract(covered).max(BigDecimal.ZERO);
        if (residual.signum() > 0) {
          residualTemplate.put(entry.getKey(), residual);
        }
      }

      BigDecimal templateTotal = residualTemplate.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
      if (templateTotal.signum() > 0) {
        for (var entry : residualTemplate.entrySet()) {
          BigDecimal adjustedWeight = remaining.multiply(entry.getValue()).divide(templateTotal, 8, RoundingMode.HALF_UP);
          merged.merge(entry.getKey(), adjustedWeight, BigDecimal::add);
        }
      } else {
        merged.merge(unclassifiedLabel(type), remaining, BigDecimal::add);
      }
    }

    normalizeWeightDrift(merged);

    BigDecimal lookThroughCoverage = resolveLookThroughCoverage(position, holdingCoverage, true);
    String warning = null;
    if (holdingCoverage.compareTo(HUNDRED) < 0) {
      warning = "Only " + displayPercent(holdingCoverage) + "% of holdings had " + type.name().toLowerCase() + " metadata; the remaining "
          + displayPercent(HUNDRED.subtract(holdingCoverage)) + "% used ETF-level exposure estimates.";
    }

    return new ExposureResolution(merged, holdingCoverage, lookThroughCoverage, resolveSourceType(position, lookThroughCoverage, holdingCoverage), warning);
  }

  private BigDecimal resolveLookThroughCoverage(Position position, BigDecimal holdingCoverage, boolean hasGranularExposure) {
    if (!hasGranularExposure) {
      return BigDecimal.ZERO;
    }

    BigDecimal metadataCoverage = position.getEtf().getLookThroughCoverage() == null
        ? BigDecimal.ZERO
        : position.getEtf().getLookThroughCoverage();

    if (metadataCoverage.signum() > 0) {
      return metadataCoverage.max(holdingCoverage).min(HUNDRED);
    }

    return holdingCoverage.min(HUNDRED);
  }

  private String resolveSourceType(Position position, BigDecimal lookThroughCoverage, BigDecimal holdingCoverage) {
    String metadataSource = position.getEtf().getMetadataSource();
    if (metadataSource != null && !metadataSource.isBlank()) {
      return metadataSource;
    }
    if (lookThroughCoverage.compareTo(holdingCoverage) > 0) {
      return "local-metadata";
    }
    return holdingCoverage.signum() > 0 ? "look-through" : "estimated";
  }

  private Map<String, BigDecimal> aggregateHoldingWeights(
      List<EtfHolding> holdings,
      java.util.function.Function<EtfHolding, String> holdingLabelExtractor
  ) {
    Map<String, BigDecimal> weights = new LinkedHashMap<>();
    for (EtfHolding holding : holdings) {
      String label = normalizeHoldingLabel(holdingLabelExtractor.apply(holding));
      if (label == null) {
        continue;
      }
      weights.merge(label, holding.getWeight(), BigDecimal::add);
    }
    return weights;
  }

  private Map<String, BigDecimal> aggregateExposureWeights(ExposureType type, List<com.etfcompass.backend.domain.EtfExposure> exposures) {
    Map<String, BigDecimal> weights = new LinkedHashMap<>();
    for (var exposure : exposures) {
      String label = sanitizeExposureLabel(type, exposure.getName());
      weights.merge(label, exposure.getWeight(), BigDecimal::add);
    }
    return weights;
  }

  private void normalizeWeightDrift(Map<String, BigDecimal> weights) {
    BigDecimal total = weights.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal drift = HUNDRED.subtract(total);
    if (drift.abs().compareTo(BigDecimal.valueOf(0.0001)) <= 0) {
      return;
    }

    String largestKey = weights.entrySet().stream()
        .max(Map.Entry.comparingByValue())
        .map(Map.Entry::getKey)
        .orElse(null);
    if (largestKey != null) {
      weights.merge(largestKey, drift, BigDecimal::add);
    }
  }

  private void mergeHoldingBreakdown(
      Map<String, BigDecimal> amounts,
      List<EtfHolding> holdings,
      BigDecimal positionValue,
      ExposureType type,
      java.util.function.Function<EtfHolding, String> holdingLabelExtractor
  ) {
    BigDecimal classifiedWeight = BigDecimal.ZERO;
    for (EtfHolding holding : holdings) {
      String label = normalizeHoldingLabel(holdingLabelExtractor.apply(holding));
      if (label == null) {
        continue;
      }
      classifiedWeight = classifiedWeight.add(holding.getWeight());
      amounts.merge(label, weighted(positionValue, holding.getWeight()), BigDecimal::add);
    }

    BigDecimal remainingWeight = HUNDRED.subtract(classifiedWeight).max(BigDecimal.ZERO);
    if (remainingWeight.signum() > 0) {
      amounts.merge(unclassifiedLabel(type), weighted(positionValue, remainingWeight), BigDecimal::add);
    }
  }

  private String normalizeHoldingLabel(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim();
  }

  private String sanitizeExposureLabel(ExposureType type, String rawLabel) {
    String label = safe(rawLabel, unclassifiedLabel(type)).trim();
    String normalized = label.toLowerCase();
    if (normalized.equals("unknown")
        || normalized.equals("other")
        || normalized.equals("other industries")
        || normalized.equals("other geography")
        || normalized.equals("other commodities")) {
      return unclassifiedLabel(type);
    }
    return label;
  }

  private boolean isUnclassifiedLabel(String label) {
    String normalized = label == null ? "" : label.trim().toLowerCase();
    return normalized.startsWith("unclassified") || normalized.equals("unknown") || normalized.equals("other");
  }

  private String unclassifiedLabel(ExposureType type) {
    return switch (type) {
      case COUNTRY -> "Unclassified geography";
      case SECTOR -> "Unclassified sectors";
      case INDUSTRY -> "Unclassified industries";
      case CURRENCY -> "Unclassified currency";
      default -> "Unclassified";
    };
  }

  private String positionLabel(PositionResponse position) {
    String ticker = safe(position.ticker(), "ETF");
    String name = safe(position.name(), ticker);
    return ticker.equalsIgnoreCase(name) ? ticker : name + " (" + ticker + ")";
  }

  private BigDecimal weighted(BigDecimal value, BigDecimal weight) {
    return value.multiply(weight).divide(BigDecimal.valueOf(100), 8, RoundingMode.HALF_UP);
  }

  private BigDecimal percentage(BigDecimal part, BigDecimal total) {
    return total.signum() == 0 ? BigDecimal.ZERO : part.multiply(BigDecimal.valueOf(100)).divide(total, 4, RoundingMode.HALF_UP);
  }

  private String safe(String value, String fallback) {
    return value == null || value.isBlank() ? fallback : value;
  }

  private String displayPercent(BigDecimal value) {
    return value.setScale(1, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
  }

  private record HoldingAmount(String name, BigDecimal value) {}

  private record ExposureResolution(
      Map<String, BigDecimal> weights,
      BigDecimal holdingCoveragePercentage,
      BigDecimal lookThroughCoveragePercentage,
      String sourceType,
      String warningReason
  ) {}

  private record ExposureAnalysis(
      List<AllocationSliceResponse> slices,
      ExposureDataQualityResponse dataQuality
  ) {}
}
