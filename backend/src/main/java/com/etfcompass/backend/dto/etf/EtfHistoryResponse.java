package com.etfcompass.backend.dto.etf;

import java.util.List;

public record EtfHistoryResponse(
    String ticker,
    String range,
    List<EtfHistoryPointResponse> points
) {}
