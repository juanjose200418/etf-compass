package com.etfcompass.backend.service;

import com.etfcompass.backend.config.MarketDataProperties;
import com.etfcompass.backend.dto.etf.QuoteSnapshotResponse;
import com.etfcompass.backend.exception.BadRequestException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class FmpQuoteProvider extends BaseMarketDataProvider implements MarketQuoteProvider {

  private static final String BASE_URL = "https://financialmodelingprep.com";

  private final MarketDataProperties properties;

  public FmpQuoteProvider(MarketDataProperties properties, RestClient.Builder restClientBuilder, ObjectMapper objectMapper) {
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
  public QuoteResult fetchQuote(String ticker) {
    if (!isConfigured()) {
      throw new BadRequestException("Missing FMP_API_KEY. Configure a valid Financial Modeling Prep token in the backend environment.");
    }

    RestClient client = restClientBuilder.baseUrl(BASE_URL).build();
    List<String> warnings = new ArrayList<>();
    JsonNode quote = requestJson(client, "/stable/quote", Map.of("symbol", normalizeTicker(ticker)), "apikey", properties.fmpApiKey(), true, warnings, "quote");
    JsonNode quoteNode = firstObject(quote);

    QuoteSnapshotResponse response = new QuoteSnapshotResponse(
        firstDecimalOrNull(quoteNode, "price"),
        firstDecimalOrNull(quoteNode, "change"),
        firstDecimalOrNull(quoteNode, "changesPercentage"),
        firstDecimalOrNull(quoteNode, "dayHigh", "high"),
        firstDecimalOrNull(quoteNode, "dayLow", "low"),
        firstDecimalOrNull(quoteNode, "open"),
        firstDecimalOrNull(quoteNode, "previousClose")
    );

    return new QuoteResult(response, List.copyOf(new LinkedHashSet<>(warnings)));
  }
}
