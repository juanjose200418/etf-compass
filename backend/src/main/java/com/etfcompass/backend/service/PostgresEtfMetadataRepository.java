package com.etfcompass.backend.service;

import com.etfcompass.backend.domain.Etf;
import com.etfcompass.backend.repository.EtfRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PostgresEtfMetadataRepository implements EtfMetadataRepository {

  private final EtfRepository etfRepository;

  @Override
  public Optional<Etf> findByTicker(String ticker) {
    return etfRepository.findByTickerIgnoreCase(ticker);
  }
}
