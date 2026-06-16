package com.etfcompass.backend.service;

import com.etfcompass.backend.config.MarketDataProperties;
import com.etfcompass.backend.dto.etf.EtfHistoryResponse;
import com.etfcompass.backend.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EtfHistoryService {

  private final MarketDataProperties properties;
  private final FinnhubMarketDataProvider finnhubProvider;
  private final FmpMarketDataProvider fmpProvider;

  public EtfHistoryResponse getHistory(String ticker, String range) {
    EtfHistoryRange resolvedRange;
    try {
      resolvedRange = EtfHistoryRange.from(range);
    } catch (IllegalArgumentException ex) {
      throw new BadRequestException(ex.getMessage());
    }

    return switch (properties.normalizedProvider()) {
      case "finnhub" -> withOptionalFallback(finnhubProvider, fmpProvider, ticker, resolvedRange);
      case "fmp" -> withOptionalFallback(fmpProvider, finnhubProvider, ticker, resolvedRange);
      default -> throw new BadRequestException("Unsupported market data provider: " + properties.normalizedProvider());
    };
  }

  private EtfHistoryResponse withOptionalFallback(
      FmpMarketDataProvider primary,
      FinnhubMarketDataProvider secondary,
      String ticker,
      EtfHistoryRange range
  ) {
    try {
      EtfHistoryResponse primaryResponse = primary.fetchHistory(ticker, range);
      if (!isEmpty(primaryResponse) || !secondary.isConfigured()) {
        return primaryResponse;
      }

      EtfHistoryResponse secondaryResponse = secondary.fetchHistory(ticker, range);
      return isEmpty(secondaryResponse) ? primaryResponse : secondaryResponse;
    } catch (BadRequestException primaryError) {
      if (secondary.isConfigured()) {
        return secondary.fetchHistory(ticker, range);
      }
      throw primaryError;
    }
  }

  private EtfHistoryResponse withOptionalFallback(
      FinnhubMarketDataProvider primary,
      FmpMarketDataProvider secondary,
      String ticker,
      EtfHistoryRange range
  ) {
    try {
      EtfHistoryResponse primaryResponse = primary.fetchHistory(ticker, range);
      if (!isEmpty(primaryResponse) || !secondary.isConfigured()) {
        return primaryResponse;
      }

      EtfHistoryResponse secondaryResponse = secondary.fetchHistory(ticker, range);
      return isEmpty(secondaryResponse) ? primaryResponse : secondaryResponse;
    } catch (BadRequestException primaryError) {
      if (secondary.isConfigured()) {
        return secondary.fetchHistory(ticker, range);
      }
      throw primaryError;
    }
  }

  private boolean isEmpty(EtfHistoryResponse response) {
    return response == null || response.points() == null || response.points().isEmpty();
  }
}
