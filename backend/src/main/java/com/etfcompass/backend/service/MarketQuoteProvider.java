package com.etfcompass.backend.service;

public interface MarketQuoteProvider {
  String providerName();
  boolean isConfigured();
  QuoteResult fetchQuote(String ticker);
}
