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
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "investment_transactions")
@NoArgsConstructor
public class InvestmentTransaction extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "position_id", nullable = false)
  private Position position;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 40)
  private TransactionType type;

  @Column(nullable = false)
  private LocalDate transactionDate;

  @Column(precision = 24, scale = 8)
  private BigDecimal quantity;
  @Column(precision = 20, scale = 6)
  private BigDecimal price;
  @Column(precision = 20, scale = 6)
  private BigDecimal fees;
  @Column(nullable = false, length = 12)
  private String currency;
}
