package com.etfcompass.backend.controller;

import com.etfcompass.backend.domain.Broker;
import com.etfcompass.backend.dto.ApiResponse;
import com.etfcompass.backend.dto.imports.ImportJobResponse;
import com.etfcompass.backend.service.ImportService;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ImportController {

  private final ImportService importService;

  @GetMapping("/imports/brokers")
  ApiResponse<List<Broker>> supportedBrokers() {
    return ApiResponse.ok(Arrays.asList(Broker.values()));
  }

  @GetMapping("/imports")
  ApiResponse<List<ImportJobResponse>> imports(@AuthenticationPrincipal UserDetails userDetails) {
    return ApiResponse.ok(importService.list(userDetails.getUsername()));
  }

  @PostMapping(value = "/portfolios/{portfolioId}/imports/csv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  ResponseEntity<ApiResponse<ImportJobResponse>> importCsv(
      @AuthenticationPrincipal UserDetails userDetails,
      @PathVariable UUID portfolioId,
      @RequestParam(defaultValue = "MANUAL") Broker broker,
      @RequestParam MultipartFile file
  ) {
    return ResponseEntity.status(201).body(ApiResponse.created(importService.importCsv(userDetails.getUsername(), portfolioId, broker, file)));
  }
}
