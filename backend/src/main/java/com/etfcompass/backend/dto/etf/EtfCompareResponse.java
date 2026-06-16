package com.etfcompass.backend.dto.etf;

import java.util.List;

public record EtfCompareResponse(
    List<EtfResponse> etfs,
    List<String> warnings
) {}
