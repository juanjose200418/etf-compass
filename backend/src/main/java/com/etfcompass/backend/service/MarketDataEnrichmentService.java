package com.etfcompass.backend.service;

import com.etfcompass.backend.config.MarketDataProperties;
import com.etfcompass.backend.domain.Etf;
import com.etfcompass.backend.domain.EtfExposure;
import com.etfcompass.backend.domain.EtfHolding;
import com.etfcompass.backend.domain.ExposureType;
import com.etfcompass.backend.repository.EtfExposureRepository;
import com.etfcompass.backend.repository.EtfHoldingRepository;
import com.etfcompass.backend.repository.EtfRepository;
import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class MarketDataEnrichmentService {

  private static final String FMP_BASE_URL = "https://financialmodelingprep.com";
  private static final List<String> EUROPEAN_SUFFIXES = List.of("", ".DE", ".AS", ".L", ".MI", ".PA", ".SW", ".BR", ".MC");
  private static final int MAX_HOLDINGS = 50;

  private final MarketDataProperties properties;
  private final EtfRepository etfRepository;
  private final EtfExposureRepository exposureRepository;
  private final EtfHoldingRepository holdingRepository;
  private final RestClient.Builder restClientBuilder;

  @Transactional
  public void enrichIfConfigured(Etf etf) {
    if (!properties.enrichOnPositionCreate() || !properties.usesFmp() || !properties.hasFmpKey()) {
      return;
    }

    try {
      enrichFromFmp(etf);
    } catch (RuntimeException ignored) {
      // Portfolio creation must not fail because a market-data provider is down or rate-limited.
    }
  }

  @Transactional
  public void refreshIfConfigured(Etf etf) {
    if (!properties.refreshOnRead() || !properties.usesFmp() || !properties.hasFmpKey() || !isRefreshDue(etf)) {
      return;
    }

    try {
      enrichFromFmp(etf);
    } catch (RuntimeException ignored) {
      // Read flows should gracefully fall back to the last persisted ETF snapshot.
    }
  }

  private void enrichFromFmp(Etf etf) {
    RestClient client = restClientBuilder.baseUrl(FMP_BASE_URL).build();
    ProviderSnapshot snapshot = resolveProviderSnapshot(client, etf.getTicker());
    if (snapshot == null) {
      return;
    }

    if (etf.getId() == null) {
      etfRepository.save(etf);
    }

    updateEtfCore(etf, snapshot);

    List<EtfExposure> sectorExposures = fetchAllocations(snapshot.sectorWeights(), ExposureType.SECTOR, "sector", "sector", "weightPercentage", "weight");
    List<EtfExposure> countryExposures = fetchAllocations(snapshot.countryWeights(), ExposureType.COUNTRY, "country", "country", "weightPercentage", "weight");
    List<EtfHolding> holdings = fetchHoldings(client, snapshot.holdings());
    List<EtfExposure> industryExposures = deriveIndustryExposures(holdings);

    if (sectorExposures.isEmpty() && countryExposures.isEmpty() && holdings.isEmpty() && industryExposures.isEmpty()) {
      etfRepository.save(etf);
      return;
    }

    exposureRepository.deleteByEtf_Id(etf.getId());
    holdingRepository.deleteByEtf_Id(etf.getId());

    sectorExposures.forEach(exposure -> exposure.setEtf(etf));
    countryExposures.forEach(exposure -> exposure.setEtf(etf));
    industryExposures.forEach(exposure -> exposure.setEtf(etf));
    holdingExposure(etf).forEach(exposureRepository::save);
    exposureRepository.saveAll(sectorExposures);
    exposureRepository.saveAll(countryExposures);
    exposureRepository.saveAll(industryExposures);

    holdings.forEach(holding -> holding.setEtf(etf));
    holdingRepository.saveAll(holdings);
    etfRepository.save(etf);
  }

  private ProviderSnapshot resolveProviderSnapshot(RestClient client, String ticker) {
    LocalDate today = LocalDate.now(ZoneOffset.UTC);
    String from = today.minusYears(6).toString();
    String to = today.toString();

    for (String suffix : EUROPEAN_SUFFIXES) {
      String candidate = ticker.toUpperCase(Locale.ROOT) + suffix;
      JsonNode info = get(client, "/stable/etf/info", Map.of("symbol", candidate));
      JsonNode sectors = get(client, "/stable/etf/sector-weightings", Map.of("symbol", candidate));
      JsonNode countries = get(client, "/stable/etf/country-weightings", Map.of("symbol", candidate));
      JsonNode holdings = get(client, "/stable/etf/holdings", Map.of("symbol", candidate));
      JsonNode history = get(client, "/stable/historical-price-eod/light", Map.of("symbol", candidate, "from", from, "to", to));
      if (hasObjectData(info) || hasArrayData(sectors) || hasArrayData(countries) || hasArrayData(holdings) || hasArrayData(extractArray(history, "historical", "data"))) {
        return new ProviderSnapshot(candidate, firstObject(info), sectors, countries, holdings, history);
      }
    }
    return null;
  }

  private List<EtfExposure> fetchAllocations(
      JsonNode root,
      ExposureType type,
      String labelField,
      String... weightFields
  ) {
    if (!hasArrayData(root)) {
      return List.of();
    }

    List<EtfExposure> exposures = new ArrayList<>();
    for (JsonNode node : root) {
      String label = firstText(node, labelField, "name");
      BigDecimal weight = parsePercent(firstText(node, weightFields));
      if (!label.isBlank() && weight.signum() > 0) {
        EtfExposure exposure = new EtfExposure();
        exposure.setType(type);
        exposure.setName(normalizeLabel(label));
        exposure.setWeight(weight);
        exposures.add(exposure);
      }
    }
    return normalizeWeights(exposures);
  }

  private List<EtfHolding> fetchHoldings(RestClient client, JsonNode root) {
    if (!hasArrayData(root)) {
      return List.of();
    }

    List<EtfHolding> holdings = new ArrayList<>();
    int count = 0;
    for (JsonNode node : root) {
      if (count++ >= MAX_HOLDINGS) {
        break;
      }
      String holdingSymbol = firstText(node, "asset", "symbol", "ticker");
      String name = firstText(node, "name", "description");
      BigDecimal weight = parsePercent(firstText(node, "weightPercentage", "weight", "percentage"));
      if (holdingSymbol.isBlank() || weight.signum() <= 0) {
        continue;
      }
      CompanyProfile profile = fetchCompanyProfile(client, holdingSymbol);
      EtfHolding holding = new EtfHolding();
      holding.setSymbol(holdingSymbol.toUpperCase(Locale.ROOT));
      holding.setName(name.isBlank() ? holding.getSymbol() : name);
      holding.setCountry(profile.country());
      holding.setSector(profile.sector());
      holding.setIndustry(profile.industry());
      holding.setWeight(weight);
      holdings.add(holding);
    }
    return holdings;
  }

  private List<EtfExposure> deriveIndustryExposures(List<EtfHolding> holdings) {
    Map<String, BigDecimal> weights = new LinkedHashMap<>();
    BigDecimal classified = BigDecimal.ZERO;
    for (EtfHolding holding : holdings) {
      String industry = holding.getIndustry();
      if (industry == null || industry.isBlank()) {
        continue;
      }
      classified = classified.add(holding.getWeight());
      weights.merge(normalizeLabel(industry), holding.getWeight(), BigDecimal::add);
    }
    if (weights.isEmpty()) {
      return List.of();
    }
    if (classified.compareTo(BigDecimal.valueOf(100)) < 0) {
      weights.merge("Unclassified industries", BigDecimal.valueOf(100).subtract(classified), BigDecimal::add);
    }
    return weights.entrySet().stream()
        .map(entry -> exposure(ExposureType.INDUSTRY, entry.getKey(), entry.getValue()))
        .toList();
  }

  private List<EtfExposure> holdingExposure(Etf etf) {
    EtfExposure exposure = new EtfExposure();
    exposure.setEtf(etf);
    exposure.setType(ExposureType.ASSET_CLASS);
    exposure.setName(etf.getAssetClass() == null || etf.getAssetClass().isBlank() ? "Equity" : etf.getAssetClass());
    exposure.setWeight(BigDecimal.valueOf(100));
    return List.of(exposure);
  }

  private CompanyProfile fetchCompanyProfile(RestClient client, String symbol) {
    JsonNode root = get(client, "/stable/profile", Map.of("symbol", symbol));
    JsonNode node = firstObject(root);
    if (node == null || node.isMissingNode()) {
      return new CompanyProfile("", "", "");
    }
    return new CompanyProfile(
        normalizeCountry(firstText(node, "country")),
        normalizeLabel(firstText(node, "sector")),
        normalizeLabel(firstText(node, "industry"))
    );
  }

  private void updateEtfCore(Etf etf, ProviderSnapshot snapshot) {
    JsonNode info = snapshot.info();
    etf.setTicker(snapshot.symbol());
    etf.setName(firstNonBlank(
        firstText(info, "name", "fundName", "etfName"),
        etf.getName(),
        snapshot.symbol()));
    etf.setProvider(firstNonBlank(firstText(info, "issuer", "provider", "brand", "family"), etf.getProvider()));
    etf.setIsin(firstNonBlank(firstText(info, "isin"), etf.getIsin()));

    BigDecimal ter = firstDecimal(info, "expenseRatio", "expense_ratio", "ter");
    if (ter.signum() > 0) {
      etf.setTer(ter.setScale(4, RoundingMode.HALF_UP));
    }

    BigDecimal aum = firstDecimal(info, "aum", "assetsUnderManagement", "totalAssets", "netAssets");
    if (aum.signum() > 0) {
      etf.setAssetsUnderManagement(aum.setScale(2, RoundingMode.HALF_UP));
    }

    etf.setCurrency(firstNonBlank(firstText(info, "currency", "tradingCurrency"), etf.getCurrency()));
    etf.setAssetClass(firstNonBlank(firstText(info, "assetClass", "asset_class"), inferAssetClass(snapshot.info(), snapshot.sectorWeights(), snapshot.holdings()), etf.getAssetClass()));
    etf.setRegion(firstNonBlank(firstText(info, "region", "geographyFocus", "focus"), inferRegion(snapshot.countryWeights(), info), etf.getRegion()));
    etf.setBenchmarkIndex(firstNonBlank(firstText(info, "benchmark", "benchmarkIndex", "indexTracked", "underlyingIndex"), etf.getBenchmarkIndex()));

    PriceHistory history = parseHistory(snapshot.history());
    if (history.latestPrice().signum() > 0) {
      etf.setLastPrice(history.latestPrice());
      etf.setLastPriceAt(history.latestDate().atStartOfDay().toInstant(ZoneOffset.UTC));
      etf.setPerformance1y(calculatePerformance(history.points(), 1));
      etf.setPerformance3y(calculatePerformance(history.points(), 3));
      etf.setPerformance5y(calculatePerformance(history.points(), 5));
    }
  }

  private JsonNode get(RestClient client, String path, Map<String, String> params) {
    return client.get()
        .uri(uriBuilder -> {
          var builder = uriBuilder.path(path);
          params.forEach(builder::queryParam);
          return builder.queryParam("apikey", properties.fmpApiKey()).build();
        })
        .retrieve()
        .body(JsonNode.class);
  }

  private boolean hasArrayData(JsonNode node) {
    return node != null && node.isArray() && !node.isEmpty();
  }

  private boolean hasObjectData(JsonNode node) {
    JsonNode object = firstObject(node);
    return object != null && object.isObject() && object.size() > 0;
  }

  private String firstText(JsonNode node, String... fields) {
    if (node == null) {
      return "";
    }
    for (String field : fields) {
      JsonNode value = node.get(field);
      if (value != null && !value.isNull()) {
        String text = value.asText("").trim();
        if (!text.isBlank()) {
          return text;
        }
      }
    }
    return "";
  }

  private BigDecimal firstDecimal(JsonNode node, String... fields) {
    String raw = firstText(node, fields);
    if (raw.isBlank()) {
      return BigDecimal.ZERO;
    }
    try {
      return new BigDecimal(raw.replace("%", "").replace(",", "").trim());
    } catch (NumberFormatException ex) {
      return BigDecimal.ZERO;
    }
  }

  private BigDecimal parsePercent(String value) {
    if (value == null || value.isBlank()) {
      return BigDecimal.ZERO;
    }
    try {
      return new BigDecimal(value.replace("%", "").replace(",", ".").trim()).setScale(4, RoundingMode.HALF_UP);
    } catch (NumberFormatException ex) {
      return BigDecimal.ZERO;
    }
  }

  private JsonNode firstObject(JsonNode root) {
    if (root == null || root.isNull()) {
      return null;
    }
    if (root.isArray()) {
      return root.isEmpty() ? null : root.get(0);
    }
    return root;
  }

  private JsonNode extractArray(JsonNode root, String... fieldNames) {
    if (root == null || root.isNull()) {
      return null;
    }
    if (root.isArray()) {
      return root;
    }
    for (String fieldName : fieldNames) {
      JsonNode child = root.get(fieldName);
      if (child != null && child.isArray()) {
        return child;
      }
    }
    return null;
  }

  private PriceHistory parseHistory(JsonNode root) {
    JsonNode items = extractArray(root, "historical", "data");
    if (!hasArrayData(items)) {
      return new PriceHistory(List.of(), BigDecimal.ZERO, LocalDate.now(ZoneOffset.UTC));
    }

    List<PricePoint> points = new ArrayList<>();
    for (JsonNode node : items) {
      String dateText = firstText(node, "date");
      BigDecimal close = firstDecimal(node, "close", "price", "adjClose", "adjustedClose");
      if (dateText.isBlank() || close.signum() <= 0) {
        continue;
      }
      try {
        points.add(new PricePoint(LocalDate.parse(dateText), close));
      } catch (RuntimeException ignored) {
        // Ignore malformed dates from the provider.
      }
    }

    points.sort(Comparator.comparing(PricePoint::date));
    if (points.isEmpty()) {
      return new PriceHistory(List.of(), BigDecimal.ZERO, LocalDate.now(ZoneOffset.UTC));
    }

    PricePoint latest = points.get(points.size() - 1);
    return new PriceHistory(points, latest.close(), latest.date());
  }

  private BigDecimal calculatePerformance(List<PricePoint> points, int years) {
    if (points.size() < 2) {
      return BigDecimal.ZERO;
    }

    PricePoint latest = points.get(points.size() - 1);
    LocalDate targetDate = latest.date().minusYears(years);
    PricePoint closest = points.stream()
        .min(Comparator.comparingLong(point -> Math.abs(ChronoUnit.DAYS.between(point.date(), targetDate))))
        .orElse(points.get(0));

    if (closest.close().signum() <= 0 || latest.close().signum() <= 0) {
      return BigDecimal.ZERO;
    }

    return latest.close()
        .divide(closest.close(), 8, RoundingMode.HALF_UP)
        .subtract(BigDecimal.ONE)
        .multiply(BigDecimal.valueOf(100))
        .setScale(4, RoundingMode.HALF_UP);
  }

  private boolean isRefreshDue(Etf etf) {
    if (etf.getLastPrice() == null || etf.getPerformance1y() == null || etf.getProvider() == null || etf.getProvider().isBlank()) {
      return true;
    }

    if (properties.refreshMaxAgeHours() <= 0) {
      return true;
    }

    Instant updatedAt = etf.getUpdatedAt();
    if (updatedAt == null) {
      return true;
    }
    return updatedAt.isBefore(Instant.now().minus(properties.refreshMaxAgeHours(), ChronoUnit.HOURS));
  }

  private String firstNonBlank(String... values) {
    for (String value : values) {
      if (value != null && !value.isBlank()) {
        return value.trim();
      }
    }
    return "";
  }

  private String inferAssetClass(JsonNode info, JsonNode sectorWeights, JsonNode holdings) {
    String text = (firstText(info, "name", "description", "focus") + " " + firstText(firstObject(sectorWeights), "sector") + " " + firstText(firstObject(holdings), "name"))
        .toLowerCase(Locale.ROOT);
    if (text.contains("bond") || text.contains("treasury") || text.contains("fixed income")) return "Fixed Income";
    if (text.contains("real estate") || text.contains("reit")) return "Real Estate";
    if (text.contains("commodity") || text.contains("gold") || text.contains("silver") || text.contains("oil")) return "Commodity";
    return text.isBlank() ? "" : "Equity";
  }

  private String inferRegion(JsonNode countryWeights, JsonNode info) {
    if (hasArrayData(countryWeights)) {
      String topCountry = firstText(countryWeights.get(0), "country", "name");
      if (!topCountry.isBlank()) {
        return normalizeCountry(topCountry);
      }
    }

    String text = firstText(info, "focus", "region", "description").toLowerCase(Locale.ROOT);
    if (text.contains("global") || text.contains("world")) return "Global";
    if (text.contains("emerging")) return "Emerging Markets";
    if (text.contains("europe")) return "Europe";
    if (text.contains("asia")) return "Asia-Pacific";
    if (text.contains("united states") || text.contains("u.s.") || text.contains("usa")) return "United States";
    return "";
  }

  private List<EtfExposure> normalizeWeights(List<EtfExposure> exposures) {
    BigDecimal total = exposures.stream().map(EtfExposure::getWeight).reduce(BigDecimal.ZERO, BigDecimal::add);
    if (total.signum() == 0 || total.compareTo(BigDecimal.valueOf(99)) >= 0 && total.compareTo(BigDecimal.valueOf(101)) <= 0) {
      return exposures;
    }
    exposures.forEach(exposure -> exposure.setWeight(exposure.getWeight().multiply(BigDecimal.valueOf(100)).divide(total, 4, RoundingMode.HALF_UP)));
    return exposures;
  }

  private EtfExposure exposure(ExposureType type, String name, BigDecimal weight) {
    EtfExposure exposure = new EtfExposure();
    exposure.setType(type);
    exposure.setName(name);
    exposure.setWeight(weight);
    return exposure;
  }

  private String normalizeLabel(String value) {
    if (value == null || value.isBlank()) {
      return "";
    }
    return value.trim();
  }

  private String normalizeCountry(String value) {
    return switch (value == null ? "" : value.trim().toUpperCase(Locale.ROOT)) {
      case "US", "USA", "UNITED STATES" -> "United States";
      case "GB", "UK", "UNITED KINGDOM" -> "United Kingdom";
      case "CN", "CHINA" -> "China";
      case "JP", "JAPAN" -> "Japan";
      case "DE", "GERMANY" -> "Germany";
      case "FR", "FRANCE" -> "France";
      case "NL", "NETHERLANDS" -> "Netherlands";
      case "TW", "TAIWAN" -> "Taiwan";
      case "CA", "CANADA" -> "Canada";
      default -> value == null ? "" : value.trim();
    };
  }

  private record CompanyProfile(String country, String sector, String industry) {}

  private record ProviderSnapshot(
      String symbol,
      JsonNode info,
      JsonNode sectorWeights,
      JsonNode countryWeights,
      JsonNode holdings,
      JsonNode history
  ) {}

  private record PricePoint(LocalDate date, BigDecimal close) {}

  private record PriceHistory(List<PricePoint> points, BigDecimal latestPrice, LocalDate latestDate) {}
}
