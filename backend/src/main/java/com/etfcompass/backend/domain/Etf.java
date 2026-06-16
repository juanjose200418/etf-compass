package com.etfcompass.backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "etfs")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Etf extends BaseEntity {

  @Column(nullable = false, unique = true, length = 32)
  private String ticker;

  @Column(unique = true, length = 32)
  private String isin;

  @Column(nullable = false, length = 240)
  private String name;

  @Column(length = 120)
  private String provider;

  @Column(precision = 8, scale = 4)
  private BigDecimal ter;

  @Column(precision = 20, scale = 2)
  private BigDecimal assetsUnderManagement;

  @Column(length = 12)
  private String currency;

  @Column(length = 80)
  private String assetClass;

  @Column(length = 80)
  private String region;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private DistributionPolicy distributionPolicy = DistributionPolicy.ACCUMULATING;

  @Column(length = 180)
  private String benchmarkIndex;

  private Integer riskLevel;

  @Column(precision = 20, scale = 6)
  private BigDecimal lastPrice;
  private Instant lastPriceAt;
  @Column(name = "performance_1y", precision = 10, scale = 4)
  private BigDecimal performance1y;
  @Column(name = "performance_3y", precision = 10, scale = 4)
  private BigDecimal performance3y;
  @Column(name = "performance_5y", precision = 10, scale = 4)
  private BigDecimal performance5y;

  @Column(length = 40)
  private String metadataSource;

  @Column(precision = 5, scale = 2)
  private BigDecimal lookThroughCoverage;

  private LocalDate metadataUpdatedAt;

  @OneToMany(mappedBy = "etf")
  private List<EtfExposure> exposures = new ArrayList<>();

  @OneToMany(mappedBy = "etf")
  private List<EtfHolding> holdings = new ArrayList<>();

  public Etf(String ticker, String name) {
    this.ticker = ticker.toUpperCase();
    this.name = name;
  }
}
