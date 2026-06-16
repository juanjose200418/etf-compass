package com.etfcompass.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.market-data")
public record MarketDataProperties(
    String provider,
    String finnhubApiKey,
    String fmpApiKey,
    boolean enrichOnPositionCreate,
    boolean refreshOnRead,
    int refreshMaxAgeHours
) {
  public boolean hasFinnhubKey() {
    return finnhubApiKey != null && !finnhubApiKey.isBlank();
  }

  public boolean hasFmpKey() {
    return fmpApiKey != null && !fmpApiKey.isBlank();
  }

  public String normalizedProvider() {
    return provider == null || provider.isBlank() ? "finnhub" : provider.trim().toLowerCase();
  }

  public boolean usesFinnhub() {
    return "finnhub".equals(normalizedProvider());
  }

  public boolean usesFmp() {
    return "fmp".equals(normalizedProvider());
  }
}
