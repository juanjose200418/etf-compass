package com.etfcompass.backend.service;

import com.etfcompass.backend.dto.etf.EtfResponse;
import java.util.List;

public record MarketDataResult(
    EtfResponse etf,
    List<String> warnings
) {}
