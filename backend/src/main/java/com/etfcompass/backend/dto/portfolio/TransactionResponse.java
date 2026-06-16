package com.etfcompass.backend.dto.portfolio;

import com.etfcompass.backend.domain.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record TransactionResponse(
    UUID id,
    UUID positionId,
    String ticker,
    TransactionType type,
    LocalDate transactionDate,
    BigDecimal quantity,
    BigDecimal price,
    BigDecimal fees,
    String currency
) {}
