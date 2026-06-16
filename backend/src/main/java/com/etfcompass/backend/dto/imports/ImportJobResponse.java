package com.etfcompass.backend.dto.imports;

import com.etfcompass.backend.domain.Broker;
import com.etfcompass.backend.domain.ImportStatus;
import java.time.Instant;
import java.util.UUID;

public record ImportJobResponse(
    UUID id,
    UUID portfolioId,
    Broker broker,
    ImportStatus status,
    String fileName,
    int importedPositions,
    String errorMessage,
    Instant createdAt
) {}
