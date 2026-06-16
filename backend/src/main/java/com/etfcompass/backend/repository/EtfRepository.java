package com.etfcompass.backend.repository;

import com.etfcompass.backend.domain.Etf;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EtfRepository extends JpaRepository<Etf, UUID> {
  Optional<Etf> findByTickerIgnoreCase(String ticker);
  List<Etf> findTop20ByTickerContainingIgnoreCaseOrNameContainingIgnoreCaseOrderByTickerAsc(String ticker, String name);
}
