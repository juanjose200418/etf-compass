package com.etfcompass.backend.repository;

import com.etfcompass.backend.domain.Portfolio;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PortfolioRepository extends JpaRepository<Portfolio, UUID> {
  List<Portfolio> findByUser_EmailIgnoreCaseOrderByCreatedAtDesc(String email);
  Optional<Portfolio> findByIdAndUser_EmailIgnoreCase(UUID id, String email);
}
