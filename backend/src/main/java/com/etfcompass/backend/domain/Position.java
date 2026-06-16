package com.etfcompass.backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "positions")
@NoArgsConstructor
public class Position extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "portfolio_id", nullable = false)
  private Portfolio portfolio;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "etf_id", nullable = false)
  private Etf etf;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 60)
  private Broker broker = Broker.MANUAL;

  @Column(length = 80)
  private String externalSymbol;

  @Column(nullable = false, precision = 24, scale = 8)
  private BigDecimal quantity;

  @Column(nullable = false, precision = 20, scale = 6)
  private BigDecimal averageCost;

  @Column(nullable = false, precision = 20, scale = 6)
  private BigDecimal currentPrice;

  @Column(nullable = false, length = 12)
  private String currency;

  @OneToMany(mappedBy = "position")
  private List<InvestmentTransaction> transactions = new ArrayList<>();
}
