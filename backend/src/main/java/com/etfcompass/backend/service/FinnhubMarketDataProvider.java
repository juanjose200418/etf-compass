package com.etfcompass.backend.service;

import com.etfcompass.backend.config.MarketDataProperties;
import com.etfcompass.backend.domain.DistributionPolicy;
import com.etfcompass.backend.dto.etf.EtfHistoryPointResponse;
import com.etfcompass.backend.dto.etf.EtfHistoryResponse;
import com.etfcompass.backend.dto.etf.EtfResponse;
import com.etfcompass.backend.dto.etf.PerformanceResponse;
import com.etfcompass.backend.dto.etf.QuoteSnapshotResponse;
import com.etfcompass.backend.dto.marketdata.EtfListResponse;
import com.etfcompass.backend.dto.marketdata.FinnhubEtfListItem;
import com.etfcompass.backend.exception.BadRequestException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class FinnhubMarketDataProvider extends BaseMarketDataProvider implements MarketDataProvider {

  private static final String BASE_URL = "https://finnhub.io";

  private final MarketDataProperties properties;

  public FinnhubMarketDataProvider(MarketDataProperties properties, RestClient.Builder restClientBuilder, ObjectMapper objectMapper) {
    super(restClientBuilder, objectMapper);
    this.properties = properties;
  }

  @Override
  public String providerName() {
    return "Finnhub";
  }

  @Override
  public boolean isConfigured() {
    return properties.hasFinnhubKey();
  }

  @Override
  public MarketDataResult fetchEtf(String ticker) {
    if (!isConfigured()) {
      throw new BadRequestException("Missing FINNHUB_API_KEY. Configure a valid Finnhub token in the backend environment.");
    }

    String normalized = normalizeTicker(ticker);
    RestClient client = restClientBuilder.baseUrl(BASE_URL).build();
    List<String> warnings = new ArrayList<>();

    JsonNode search = requestJson(client, "/api/v1/search", Map.of("q", normalized), "token", properties.finnhubApiKey(), true, warnings, "symbol search");
    JsonNode metrics = requestJson(client, "/api/v1/stock/metric", Map.of("symbol", normalized, "metric", "all"), "token", properties.finnhubApiKey(), true, warnings, "stock metrics");
    JsonNode etfProfile = requestJson(client, "/api/v1/etf/profile", Map.of("symbol", normalized), "token", properties.finnhubApiKey(), false, warnings, "ETF profile");

    JsonNode searchMatch = findSearchMatch(search, normalized);
    String name = firstNonBlank(firstTextOrNull(etfProfile, "name"), firstTextOrNull(searchMatch, "description"));
    if (name == null) {
      throw new BadRequestException("Finnhub no devolvio nombre para el ETF " + normalized + ".");
    }

    JsonNode metricNode = metrics.path("metric");
    BigDecimal oneYear = firstDecimalOrNull(metricNode, "52WeekPriceReturnDaily", "52WeekPriceReturn", "oneYearPriceReturnDaily", "oneYearPriceReturn");
    BigDecimal threeYear = firstDecimalOrNull(metricNode, "3YearPriceReturnDaily", "3YearAnnualizedReturn", "threeYearPriceReturnDaily", "threeYearAnnualizedReturn");
    BigDecimal fiveYear = firstDecimalOrNull(metricNode, "5YearPriceReturnDaily", "5YearAnnualizedReturn", "fiveYearPriceReturnDaily", "fiveYearAnnualizedReturn");
    if (threeYear == null || fiveYear == null) {
      warnings.add("Dato no disponible en el plan gratuito del proveedor (Finnhub performance 3Y/5Y).");
    }

    DistributionPolicy distribution = parseDistributionPolicy(firstText(etfProfile, "distributionPolicy", "distribution", "dividendPolicy"));

    EtfResponse response = new EtfResponse(
        null,
        normalized,
        name,
        firstTextOrNull(etfProfile, "isin"),
        firstTextOrNull(etfProfile, "provider", "issuer", "brand", "etfCompany"),
        firstDecimalOrNull(etfProfile, "expenseRatio", "expense_ratio", "ter", "netExpenseRatio"),
        firstTextOrNull(etfProfile, "assetClass", "asset_class"),
        firstTextOrNull(etfProfile, "region", "focus"),
        firstTextOrNull(etfProfile, "indexTracked", "benchmark", "underlyingIndex"),
        distribution,
        firstDecimalOrNull(etfProfile, "aum", "totalAssets", "netAssets"),
        firstTextOrNull(etfProfile, "currency", "tradingCurrency"),
        null,
        null,
        null,
        null,
        new PerformanceResponse(oneYear, threeYear, fiveYear),
        new QuoteSnapshotResponse(null, null, null, null, null, null, null)
    );

    return new MarketDataResult(response, List.copyOf(new LinkedHashSet<>(warnings)));
  }

  public EtfHistoryResponse fetchHistory(String ticker, EtfHistoryRange range) {
    if (!isConfigured()) {
      throw new BadRequestException("Missing FINNHUB_API_KEY. Configure a valid Finnhub token in the backend environment.");
    }

    String normalized = normalizeTicker(ticker);
    RestClient client = restClientBuilder.baseUrl(BASE_URL).build();
    List<String> warnings = new ArrayList<>();
    long to = Instant.now().getEpochSecond();
    JsonNode candles = requestJson(
        client,
        "/api/v1/stock/candle",
        Map.of(
            "symbol", normalized,
            "resolution", range.finnhubResolution(),
            "from", String.valueOf(range.fromEpochSeconds()),
            "to", String.valueOf(to)
        ),
        "token",
        properties.finnhubApiKey(),
        false,
        warnings,
        "historical prices"
    );

    if (candles.path("s").asText("").equalsIgnoreCase("no_data")) {
      return new EtfHistoryResponse(normalized, range.apiValue(), List.of());
    }

    JsonNode closes = candles.path("c");
    JsonNode timestamps = candles.path("t");
    if (!closes.isArray() || !timestamps.isArray() || closes.size() != timestamps.size()) {
      return new EtfHistoryResponse(normalized, range.apiValue(), List.of());
    }

    List<EtfHistoryPointResponse> points = new ArrayList<>();
    for (int i = 0; i < closes.size(); i++) {
      JsonNode closeNode = closes.get(i);
      JsonNode timeNode = timestamps.get(i);
      if (closeNode == null || timeNode == null || !closeNode.isNumber() || !timeNode.isNumber()) {
        continue;
      }

      BigDecimal close = closeNode.decimalValue();
      if (close.signum() <= 0) {
        continue;
      }

      LocalDate date = Instant.ofEpochSecond(timeNode.asLong()).atZone(ZoneOffset.UTC).toLocalDate();
      if (!points.isEmpty() && points.get(points.size() - 1).date().equals(date)) {
        points.set(points.size() - 1, new EtfHistoryPointResponse(date, close));
      } else {
        points.add(new EtfHistoryPointResponse(date, close));
      }
    }

    return new EtfHistoryResponse(normalized, range.apiValue(), points);
  }

  private static final long CACHE_TTL_MS = 300_000; // 5 minutos
  private volatile List<FinnhubEtfListItem> cachedEtfList;
  private volatile long cacheTimestamp;

  public EtfListResponse fetchEtfList() {
    if (!isConfigured()) {
      throw new BadRequestException("Missing FINNHUB_API_KEY. Configure a valid Finnhub token in the backend environment.");
    }

    long now = System.currentTimeMillis();
    if (cachedEtfList != null && (now - cacheTimestamp) < CACHE_TTL_MS) {
      return new EtfListResponse(cachedEtfList, cachedEtfList.size(), "Finnhub (cache)", Instant.ofEpochMilli(cacheTimestamp).toString());
    }

    RestClient client = restClientBuilder.baseUrl(BASE_URL).build();
    List<String> warnings = new ArrayList<>();
    JsonNode data = requestJson(client, "/api/v1/etf/list", Map.of(), "token", properties.finnhubApiKey(), true, warnings, "ETF list");

    List<FinnhubEtfListItem> items = new ArrayList<>();
    if (data != null && data.isArray()) {
      for (JsonNode node : data) {
        String symbol = firstTextOrNull(node, "symbol");
        if (symbol == null) continue;
        items.add(new FinnhubEtfListItem(
            symbol,
            firstTextOrNull(node, "name"),
            firstTextOrNull(node, "type"),
            firstTextOrNull(node, "mic"),
            firstTextOrNull(node, "figi"),
            firstTextOrNull(node, "isin"),
            firstTextOrNull(node, "currency")
        ));
      }
    }

    cachedEtfList = items;
    cacheTimestamp = now;
    return new EtfListResponse(items, items.size(), "Finnhub", Instant.ofEpochMilli(now).toString());
  }

  private JsonNode findSearchMatch(JsonNode search, String ticker) {
    JsonNode results = search.path("result");
    if (!results.isArray()) return objectMapper.nullNode();
    for (JsonNode result : results) {
      if (ticker.equalsIgnoreCase(firstText(result, "symbol")) && "ETP".equalsIgnoreCase(firstText(result, "type"))) return result;
    }
    for (JsonNode result : results) {
      if (ticker.equalsIgnoreCase(firstText(result, "symbol"))) return result;
    }
    return results.isEmpty() ? objectMapper.nullNode() : results.get(0);
  }
}
