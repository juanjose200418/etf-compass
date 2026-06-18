package com.etfcompass.backend.dto.marketdata;

public record FinnhubEtfListItem(
    String symbol,
    String name,
    String type,
    String mic,
    String figi,
    String isin,
    String currency
) {}