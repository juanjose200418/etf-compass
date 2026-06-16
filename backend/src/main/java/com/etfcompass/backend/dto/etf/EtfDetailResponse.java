package com.etfcompass.backend.dto.etf;

import java.util.List;

public record EtfDetailResponse(
    EtfResponse etf,
    List<ExposureResponse> exposures,
    List<HoldingResponse> holdings,
    List<String> warnings
) {}
