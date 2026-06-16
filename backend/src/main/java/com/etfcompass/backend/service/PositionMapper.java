package com.etfcompass.backend.service;

import com.etfcompass.backend.domain.Position;
import com.etfcompass.backend.dto.portfolio.PositionResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

@Component
public class PositionMapper {

  public PositionResponse toResponse(Position position) {
    BigDecimal invested = position.getQuantity().multiply(position.getAverageCost());
    BigDecimal value = position.getQuantity().multiply(position.getCurrentPrice());
    BigDecimal profitLoss = value.subtract(invested);
    BigDecimal percentage = invested.signum() == 0
        ? BigDecimal.ZERO
        : profitLoss.multiply(BigDecimal.valueOf(100)).divide(invested, 4, RoundingMode.HALF_UP);
    return new PositionResponse(
        position.getId(),
        position.getEtf().getTicker(),
        position.getEtf().getName(),
        position.getBroker(),
        position.getQuantity(),
        position.getAverageCost(),
        position.getCurrentPrice(),
        invested,
        value,
        profitLoss,
        percentage,
        position.getCurrency()
    );
  }
}
