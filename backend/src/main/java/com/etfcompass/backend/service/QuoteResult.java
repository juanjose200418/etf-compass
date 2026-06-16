package com.etfcompass.backend.service;

import com.etfcompass.backend.dto.etf.QuoteSnapshotResponse;
import java.util.List;

public record QuoteResult(
    QuoteSnapshotResponse quote,
    List<String> warnings
) {}
