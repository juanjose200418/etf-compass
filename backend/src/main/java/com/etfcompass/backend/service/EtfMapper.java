package com.etfcompass.backend.service;

import com.etfcompass.backend.domain.Etf;
import com.etfcompass.backend.domain.EtfExposure;
import com.etfcompass.backend.domain.EtfHolding;
import com.etfcompass.backend.dto.etf.EtfResponse;
import com.etfcompass.backend.dto.etf.ExposureResponse;
import com.etfcompass.backend.dto.etf.HoldingResponse;
import com.etfcompass.backend.dto.etf.PerformanceResponse;
import com.etfcompass.backend.dto.etf.QuoteSnapshotResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

@Component
public class EtfMapper {

  public EtfResponse toResponse(Etf etf) {
    return new EtfResponse(
        etf.getId(),
        etf.getTicker(),
        etf.getName(),
        etf.getIsin(),
        etf.getProvider(),
        etf.getTer(),
        etf.getAssetClass(),
        etf.getRegion(),
        etf.getBenchmarkIndex(),
        etf.getDistributionPolicy(),
        billions(etf.getAssetsUnderManagement()),
        etf.getCurrency(),
        etf.getRiskLevel(),
        etf.getMetadataSource(),
        etf.getLookThroughCoverage(),
        etf.getMetadataUpdatedAt(),
        new PerformanceResponse(null, null, null),
        new QuoteSnapshotResponse(null, null, null, null, null, null, null)
    );
  }

  public ExposureResponse toExposureResponse(EtfExposure exposure) {
    return new ExposureResponse(exposure.getType(), exposure.getName(), exposure.getWeight());
  }

  public HoldingResponse toHoldingResponse(EtfHolding holding) {
    return new HoldingResponse(
        holding.getSymbol(), holding.getName(), holding.getCountry(), holding.getSector(), holding.getIndustry(), holding.getWeight());
  }

  private BigDecimal billions(BigDecimal value) {
    return value == null ? null : value.divide(BigDecimal.valueOf(1_000_000_000L), 2, RoundingMode.HALF_UP);
  }
}
