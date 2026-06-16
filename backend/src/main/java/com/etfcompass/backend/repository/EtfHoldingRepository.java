package com.etfcompass.backend.repository;

import com.etfcompass.backend.domain.EtfHolding;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EtfHoldingRepository extends JpaRepository<EtfHolding, UUID> {
  List<EtfHolding> findByEtf_IdOrderByWeightDesc(UUID etfId);
  void deleteByEtf_Id(UUID etfId);
}
