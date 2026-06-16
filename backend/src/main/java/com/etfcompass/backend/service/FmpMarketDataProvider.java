package com.etfcompass.backend.service;

import com.etfcompass.backend.config.MarketDataProperties;
import com.etfcompass.backend.dto.etf.EtfHistoryPointResponse;
import com.etfcompass.backend.dto.etf.EtfHistoryResponse;
import com.etfcompass.backend.dto.etf.EtfResponse;
import com.etfcompass.backend.dto.etf.PerformanceResponse;
import com.etfcompass.backend.dto.etf.QuoteSnapshotResponse;
import com.etfcompass.backend.exception.BadRequestException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class FmpMarketDataProvider extends BaseMarketDataProvider implements MarketDataProvider {

  private static final String BASE_URL = "https://financialmodelingprep.com";

  private final MarketDataProperties properties;

  public FmpMarketDataProvider(MarketDataProperties properties, RestClient.Builder restClientBuilder, ObjectMapper objectMapper) {
    super(restClientBuilder, objectMapper);
    this.properties = properties;
  }

  @Override
  public String providerName() {
    return "FMP";
  }

  @Override
  public boolean isConfigured() {
    return properties.hasFmpKey() && !properties.fmpApiKey().equals(properties.finnhubApiKey());
  }

  @Override
  public MarketDataResult fetchEtf(String ticker) {
    if (!isConfigured()) {
      throw new BadRequestException("Missing FMP_API_KEY. Configure a valid Financial Modeling Prep token in the backend environment.");
    }

    String normalized = normalizeTicker(ticker);
    RestClient client = restClientBuilder.baseUrl(BASE_URL).build();
    List<String> warnings = new ArrayList<>();
    LocalDate today = LocalDate.now(ZoneOffset.UTC);

    JsonNode quote = requestJson(client, "/stable/quote", Map.of("symbol", normalized), "apikey", properties.fmpApiKey(), true, warnings, "quote");
    JsonNode history = requestJson(client, "/stable/historical-price-eod/light", Map.of("symbol", normalized, "from", today.minusYears(6).toString(), "to", today.toString()), "apikey", properties.fmpApiKey(), false, warnings, "historical prices");

    JsonNode quoteNode = firstObject(quote);
    String name = firstNonBlank(firstTextOrNull(quoteNode, "name", "companyName"), normalized);

    PriceHistory priceHistory = parseHistory(history);
    BigDecimal oneYear = calculatePerformance(priceHistory.points(), 1);
    BigDecimal threeYear = calculatePerformance(priceHistory.points(), 3);
    BigDecimal fiveYear = calculatePerformance(priceHistory.points(), 5);
    if (oneYear == null || threeYear == null || fiveYear == null) {
      warnings.add("Dato no disponible en el plan gratuito del proveedor (FMP historico suficiente para 1Y/3Y/5Y).");
    }

    EtfResponse response = new EtfResponse(
        null,
        normalized,
        name,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        firstTextOrNull(quoteNode, "currency"),
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
      throw new BadRequestException("Missing FMP_API_KEY. Configure a valid Financial Modeling Prep token in the backend environment.");
    }

    String normalized = normalizeTicker(ticker);
    RestClient client = restClientBuilder.baseUrl(BASE_URL).build();
    LocalDate today = LocalDate.now(ZoneOffset.UTC);
    JsonNode history = requestJson(
        client,
        "/stable/historical-price-eod/light",
        Map.of("symbol", normalized, "from", range.fromDate(today).toString(), "to", today.toString()),
        "apikey",
        properties.fmpApiKey(),
        false,
        new ArrayList<>(),
        "historical prices"
    );

    List<EtfHistoryPointResponse> points = parseHistory(history).points().stream()
        .map(point -> new EtfHistoryPointResponse(point.date(), point.close()))
        .toList();

    return new EtfHistoryResponse(normalized, range.apiValue(), points);
  }
}
