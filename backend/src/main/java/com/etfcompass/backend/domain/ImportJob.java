package com.etfcompass.backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "import_jobs")
@NoArgsConstructor
public class ImportJob extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private AppUser user;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "portfolio_id", nullable = false)
  private Portfolio portfolio;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 60)
  private Broker broker;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 40)
  private ImportStatus status = ImportStatus.PENDING;

  @Column(nullable = false, length = 255)
  private String fileName;

  @Column(nullable = false)
  private int importedPositions;

  @Column(length = 1000)
  private String errorMessage;
}
