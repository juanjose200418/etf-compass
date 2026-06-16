package com.etfcompass.backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "etf_exposures")
@NoArgsConstructor
public class EtfExposure extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "etf_id", nullable = false)
  private Etf etf;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 40)
  private ExposureType type;

  @Column(nullable = false, length = 140)
  private String name;

  @Column(nullable = false, precision = 9, scale = 4)
  private BigDecimal weight;
}
