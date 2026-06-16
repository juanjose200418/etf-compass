package com.etfcompass.backend.service;

import com.etfcompass.backend.domain.Etf;
import java.util.Optional;

public interface EtfMetadataRepository {
  Optional<Etf> findByTicker(String ticker);
}
