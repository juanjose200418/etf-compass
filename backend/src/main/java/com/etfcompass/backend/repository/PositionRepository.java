package com.etfcompass.backend.repository;

import com.etfcompass.backend.domain.Position;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PositionRepository extends JpaRepository<Position, UUID> {
  List<Position> findByPortfolio_IdOrderByCreatedAtAsc(UUID portfolioId);
  List<Position> findByPortfolio_IdAndPortfolio_User_EmailIgnoreCase(UUID portfolioId, String email);
  Optional<Position> findByIdAndPortfolio_User_EmailIgnoreCase(UUID id, String email);
}
