package com.etfcompass.backend.dto.marketdata;

import java.util.List;

public record EtfListResponse(
    List<FinnhubEtfListItem> etfs,
    int totalCount,
    String provider,
    String cachedAt
) {}