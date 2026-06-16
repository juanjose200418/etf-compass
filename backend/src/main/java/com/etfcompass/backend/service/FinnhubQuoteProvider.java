package com.etfcompass.backend.service;

import com.etfcompass.backend.config.MarketDataProperties;
import com.etfcompass.backend.dto.etf.QuoteSnapshotResponse;
import com.etfcompass.backend.exception.BadRequestException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class FinnhubQuoteProvider extends BaseMarketDataProvider implements MarketQuoteProvider {

  private static final String BASE_URL = "https://finnhub.io";

  private final MarketDataProperties properties;

  public FinnhubQuoteProvider(MarketDataProperties properties, RestClient.Builder restClientBuilder, ObjectMapper objectMapper) {
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
  public QuoteResult fetchQuote(String ticker) {
    if (!isConfigured()) {
      throw new BadRequestException("Missing FINNHUB_API_KEY. Configure a valid Finnhub token in the backend environment.");
    }

    RestClient client = restClientBuilder.baseUrl(BASE_URL).build();
    List<String> warnings = new ArrayList<>();
    JsonNode quote = requestJson(client, "/api/v1/quote", Map.of("symbol", normalizeTicker(ticker)), "token", properties.finnhubApiKey(), true, warnings, "quote");

    QuoteSnapshotResponse response = new QuoteSnapshotResponse(
        firstDecimalOrNull(quote, "c"),
        firstDecimalOrNull(quote, "d"),
        firstDecimalOrNull(quote, "dp"),
        firstDecimalOrNull(quote, "h"),
        firstDecimalOrNull(quote, "l"),
        firstDecimalOrNull(quote, "o"),
        firstDecimalOrNull(quote, "pc")
    );

    return new QuoteResult(response, List.copyOf(new LinkedHashSet<>(warnings)));
  }
}
