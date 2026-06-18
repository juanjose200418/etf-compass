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

  private static final Map<String, String> INDUSTRY_TO_SECTOR = Map.<String, String>ofEntries(
    Map.entry("Semiconductors", "Technology"),
    Map.entry("Software", "Technology"),
    Map.entry("Software—Application", "Technology"),
    Map.entry("Software—Infrastructure", "Technology"),
    Map.entry("Software—IT Services", "Technology"),
    Map.entry("Consumer Electronics", "Technology"),
    Map.entry("Computer Hardware", "Technology"),
    Map.entry("Electronic Components", "Technology"),
    Map.entry("Electronic Gaming & Multimedia", "Technology"),
    Map.entry("Information Technology Services", "Technology"),
    Map.entry("IT Services", "Technology"),
    Map.entry("Technology Hardware, Storage & Peripherals", "Technology"),
    Map.entry("Communication Equipment", "Technology"),
    Map.entry("Scientific & Technical Instruments", "Technology"),
    Map.entry("Solar", "Technology"),
    Map.entry("Internet Content & Information", "Technology"),
    Map.entry("Internet Retail", "Consumer Cyclical"),
    Map.entry("Internet Service Providers", "Communication Services"),
    Map.entry("Banks", "Financial Services"),
    Map.entry("Banks—Diversified", "Financial Services"),
    Map.entry("Banks—Regional", "Financial Services"),
    Map.entry("Capital Markets", "Financial Services"),
    Map.entry("Insurance", "Financial Services"),
    Map.entry("Insurance—Diversified", "Financial Services"),
    Map.entry("Insurance—Life", "Financial Services"),
    Map.entry("Insurance—Property & Casualty", "Financial Services"),
    Map.entry("Insurance—Reinsurance", "Financial Services"),
    Map.entry("Insurance—Specialty", "Financial Services"),
    Map.entry("Asset Management", "Financial Services"),
    Map.entry("Financial Conglomerates", "Financial Services"),
    Map.entry("Financial Data & Stock Exchanges", "Financial Services"),
    Map.entry("Credit Services", "Financial Services"),
    Map.entry("Mortgage Finance", "Financial Services"),
    Map.entry("Shell Companies", "Financial Services"),
    Map.entry("Health Care Plans", "Healthcare"),
    Map.entry("Healthcare", "Healthcare"),
    Map.entry("Medical Devices", "Healthcare"),
    Map.entry("Medical Instruments & Supplies", "Healthcare"),
    Map.entry("Medical Diagnostics & Research", "Healthcare"),
    Map.entry("Medical Care Facilities", "Healthcare"),
    Map.entry("Medical Distribution", "Healthcare"),
    Map.entry("Drug Manufacturers", "Healthcare"),
    Map.entry("Drug Manufacturers—General", "Healthcare"),
    Map.entry("Drug Manufacturers—Specialty & Generic", "Healthcare"),
    Map.entry("Biotechnology", "Healthcare"),
    Map.entry("Pharmaceutical Retailers", "Healthcare"),
    Map.entry("Health Information Services", "Healthcare"),
    Map.entry("Oil & Gas E&P", "Energy"),
    Map.entry("Oil & Gas Integrated", "Energy"),
    Map.entry("Oil & Gas Midstream", "Energy"),
    Map.entry("Oil & Gas Drilling", "Energy"),
    Map.entry("Oil & Gas Equipment & Services", "Energy"),
    Map.entry("Oil & Gas Refining & Marketing", "Energy"),
    Map.entry("Oil & Gas Transportation", "Energy"),
    Map.entry("Uranium", "Energy"),
    Map.entry("Coal", "Energy"),
    Map.entry("Aerospace & Defense", "Industrials"),
    Map.entry("Airlines", "Industrials"),
    Map.entry("Railroads", "Industrials"),
    Map.entry("Marine Shipping", "Industrials"),
    Map.entry("Trucking", "Industrials"),
    Map.entry("Logistics", "Industrials"),
    Map.entry("Integrated Freight & Logistics", "Industrials"),
    Map.entry("Industrial Distribution", "Industrials"),
    Map.entry("Industrial Conglomerates", "Industrials"),
    Map.entry("Machinery", "Industrials"),
    Map.entry("Farm & Heavy Construction Machinery", "Industrials"),
    Map.entry("Specialty Industrial Machinery", "Industrials"),
    Map.entry("Pollution & Treatment Controls", "Industrials"),
    Map.entry("Waste Management", "Industrials"),
    Map.entry("Engineering & Construction", "Industrials"),
    Map.entry("Building Materials", "Industrials"),
    Map.entry("Construction Materials", "Industrials"),
    Map.entry("Consulting Services", "Industrials"),
    Map.entry("Business Equipment & Supplies", "Industrials"),
    Map.entry("Security & Protection Services", "Industrials"),
    Map.entry("Staffing & Employment Services", "Industrials"),
    Map.entry("Manpower", "Industrials"),
    Map.entry("Rental & Leasing Services", "Industrials"),
    Map.entry("Electrical Equipment", "Industrials"),
    Map.entry("Specialty Business Services", "Industrials"),
    Map.entry("Farm Products", "Consumer Defensive"),
    Map.entry("Beverages—Non-Alcoholic", "Consumer Defensive"),
    Map.entry("Beverages—Alcoholic", "Consumer Defensive"),
    Map.entry("Beverages—Wineries & Distilleries", "Consumer Defensive"),
    Map.entry("Beverages—Brewers", "Consumer Defensive"),
    Map.entry("Food—Confectioners & Ingredients", "Consumer Defensive"),
    Map.entry("Food—Ingredients & Seasonings", "Consumer Defensive"),
    Map.entry("Food—Meat Products", "Consumer Defensive"),
    Map.entry("Packaged Foods", "Consumer Defensive"),
    Map.entry("Grocery Stores", "Consumer Defensive"),
    Map.entry("Discount Stores", "Consumer Defensive"),
    Map.entry("Household & Personal Products", "Consumer Defensive"),
    Map.entry("Tobacco", "Consumer Defensive"),
    Map.entry("Education & Training Services", "Consumer Defensive"),
    Map.entry("Auto Manufacturers", "Consumer Cyclical"),
    Map.entry("Auto Parts", "Consumer Cyclical"),
    Map.entry("Auto & Truck Dealerships", "Consumer Cyclical"),
    Map.entry("Recreational Vehicles", "Consumer Cyclical"),
    Map.entry("Furnishings, Fixtures & Appliances", "Consumer Cyclical"),
    Map.entry("Residential Construction", "Consumer Cyclical"),
    Map.entry("Homebuilding", "Consumer Cyclical"),
    Map.entry("Apparel Retail", "Consumer Cyclical"),
    Map.entry("Apparel Manufacturing", "Consumer Cyclical"),
    Map.entry("Footwear & Accessories", "Consumer Cyclical"),
    Map.entry("Textile Manufacturing", "Consumer Cyclical"),
    Map.entry("Specialty Retail", "Consumer Cyclical"),
    Map.entry("Department Stores", "Consumer Cyclical"),
    Map.entry("Restaurants", "Consumer Cyclical"),
    Map.entry("Travel Services", "Consumer Cyclical"),
    Map.entry("Resorts & Casinos", "Consumer Cyclical"),
    Map.entry("Lodging", "Consumer Cyclical"),
    Map.entry("Leisure", "Consumer Cyclical"),
    Map.entry("Gambling", "Consumer Cyclical"),
    Map.entry("Packaging & Containers", "Consumer Cyclical"),
    Map.entry("Personal Services", "Consumer Cyclical"),
    Map.entry("Publishing", "Communication Services"),
    Map.entry("Advertising Agencies", "Communication Services"),
    Map.entry("Media & Entertainment", "Communication Services"),
    Map.entry("Entertainment", "Communication Services"),
    Map.entry("Telecommunications", "Communication Services"),
    Map.entry("Telecom Services", "Communication Services"),
    Map.entry("Wireless Communications", "Communication Services"),
    Map.entry("Broadcasting", "Communication Services"),
    Map.entry("Cable & Other Pay Television", "Communication Services"),
    Map.entry("Social Media", "Communication Services"),
    Map.entry("Conglomerates", "Industrials"),
    Map.entry("Metals & Mining", "Basic Materials"),
    Map.entry("Steel", "Basic Materials"),
    Map.entry("Copper", "Basic Materials"),
    Map.entry("Gold", "Basic Materials"),
    Map.entry("Silver", "Basic Materials"),
    Map.entry("Other Industrial Metals & Mining", "Basic Materials"),
    Map.entry("Aluminum", "Basic Materials"),
    Map.entry("Industrial Metals & Minerals", "Basic Materials"),
    Map.entry("Specialty Chemicals", "Basic Materials"),
    Map.entry("Chemicals", "Basic Materials"),
    Map.entry("Agricultural Inputs", "Basic Materials"),
    Map.entry("Lumber & Wood Production", "Basic Materials"),
    Map.entry("Paper & Paper Products", "Basic Materials"),
    Map.entry("Forest Products", "Basic Materials"),
    Map.entry("Minerals", "Basic Materials"),
    Map.entry("Real Estate—Diversified", "Real Estate"),
    Map.entry("Real Estate—Development", "Real Estate"),
    Map.entry("Real Estate Services", "Real Estate"),
    Map.entry("REIT—Diversified", "Real Estate"),
    Map.entry("REIT—Healthcare", "Real Estate"),
    Map.entry("REIT—Hotel & Motel", "Real Estate"),
    Map.entry("REIT—Industrial", "Real Estate"),
    Map.entry("REIT—Mortgage", "Real Estate"),
    Map.entry("REIT—Office", "Real Estate"),
    Map.entry("REIT—Residential", "Real Estate"),
    Map.entry("REIT—Retail", "Real Estate"),
    Map.entry("REIT—Specialty", "Real Estate"),
    Map.entry("Utilities—Regulated Electric", "Utilities"),
    Map.entry("Utilities—Regulated Gas", "Utilities"),
    Map.entry("Utilities—Regulated Water", "Utilities"),
    Map.entry("Utilities—Independent Power Producers", "Utilities"),
    Map.entry("Utilities—Renewable", "Utilities"));

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
    Map<String, BigDecimal> holdingWeights = aggregateHoldingWeights(holdings, h -> resolveHoldingLabel(type, h, holdingLabelExtractor));
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

  private String resolveHoldingLabel(
      ExposureType type,
      EtfHolding holding,
      java.util.function.Function<EtfHolding, String> labelExtractor
  ) {
    String label = normalizeHoldingLabel(labelExtractor.apply(holding));
    if (label != null) {
      return label;
    }
    if (type == ExposureType.SECTOR) {
      String industry = normalizeHoldingLabel(holding.getIndustry());
      if (industry != null) {
        String mappedSector = INDUSTRY_TO_SECTOR.get(industry);
        if (mappedSector != null) {
          return mappedSector;
        }
      }
    }
    return null;
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
