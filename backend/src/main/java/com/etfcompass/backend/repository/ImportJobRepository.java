package com.etfcompass.backend.repository;

import com.etfcompass.backend.domain.ImportJob;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImportJobRepository extends JpaRepository<ImportJob, UUID> {
  List<ImportJob> findByUser_EmailIgnoreCaseOrderByCreatedAtDesc(String email);
}
