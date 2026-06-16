package com.etfcompass.backend.repository;

import com.etfcompass.backend.domain.EtfExposure;
import com.etfcompass.backend.domain.ExposureType;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EtfExposureRepository extends JpaRepository<EtfExposure, UUID> {
  List<EtfExposure> findByEtf_Id(UUID etfId);
  List<EtfExposure> findByEtf_IdAndType(UUID etfId, ExposureType type);
  void deleteByEtf_Id(UUID etfId);
}
