package com.etfcompass.backend.controller;

import com.etfcompass.backend.dto.ApiResponse;
import com.etfcompass.backend.dto.marketdata.EtfListResponse;
import com.etfcompass.backend.service.FinnhubMarketDataProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/market-data")
@RequiredArgsConstructor
public class MarketDataController {

  private final FinnhubMarketDataProvider finnhubMarketDataProvider;

  @GetMapping("/etfs")
  ApiResponse<EtfListResponse> listEtfs() {
    return ApiResponse.ok(finnhubMarketDataProvider.fetchEtfList());
  }
}