package com.etfcompass.backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "etf_holdings")
@NoArgsConstructor
public class EtfHolding extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "etf_id", nullable = false)
  private Etf etf;

  @Column(nullable = false, length = 40)
  private String symbol;

  @Column(nullable = false, length = 180)
  private String name;
  @Column(length = 80)
  private String country;
  @Column(length = 100)
  private String sector;
  @Column(length = 120)
  private String industry;
  @Column(nullable = false, precision = 9, scale = 4)
  private BigDecimal weight;
}
