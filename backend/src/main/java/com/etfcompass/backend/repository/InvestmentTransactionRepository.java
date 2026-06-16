package com.etfcompass.backend.repository;

import com.etfcompass.backend.domain.InvestmentTransaction;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvestmentTransactionRepository extends JpaRepository<InvestmentTransaction, UUID> {
  List<InvestmentTransaction> findByPosition_Portfolio_IdOrderByTransactionDateAsc(UUID portfolioId);
}
