package com.etfcompass.backend.service;

public interface MarketDataProvider {
  String providerName();
  boolean isConfigured();
  MarketDataResult fetchEtf(String ticker);
}
