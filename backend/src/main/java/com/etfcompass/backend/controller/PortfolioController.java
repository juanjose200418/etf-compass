package com.etfcompass.backend.controller;

import com.etfcompass.backend.dto.ApiResponse;
import com.etfcompass.backend.dto.analytics.PortfolioAnalyticsResponse;
import com.etfcompass.backend.dto.portfolio.CreatePortfolioRequest;
import com.etfcompass.backend.dto.portfolio.PortfolioResponse;
import com.etfcompass.backend.dto.portfolio.PositionRequest;
import com.etfcompass.backend.dto.portfolio.PositionResponse;
import com.etfcompass.backend.dto.portfolio.TransactionRequest;
import com.etfcompass.backend.dto.portfolio.TransactionResponse;
import com.etfcompass.backend.service.AnalyticsService;
import com.etfcompass.backend.service.PortfolioService;
import com.etfcompass.backend.service.TransactionService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/portfolios")
@RequiredArgsConstructor
public class PortfolioController {

  private final PortfolioService portfolioService;
  private final AnalyticsService analyticsService;
  private final TransactionService transactionService;

  @GetMapping
  ApiResponse<List<PortfolioResponse>> list(@AuthenticationPrincipal UserDetails userDetails) {
    return ApiResponse.ok(portfolioService.list(userDetails.getUsername()));
  }

  @PostMapping
  ResponseEntity<ApiResponse<PortfolioResponse>> create(
      @AuthenticationPrincipal UserDetails userDetails,
      @Valid @RequestBody CreatePortfolioRequest request
  ) {
    return ResponseEntity.status(201).body(ApiResponse.created(portfolioService.create(userDetails.getUsername(), request)));
  }

  @GetMapping("/{portfolioId}")
  ApiResponse<PortfolioResponse> get(@AuthenticationPrincipal UserDetails userDetails, @PathVariable UUID portfolioId) {
    return ApiResponse.ok(portfolioService.get(userDetails.getUsername(), portfolioId));
  }

  @PutMapping("/{portfolioId}")
  ApiResponse<PortfolioResponse> update(
      @AuthenticationPrincipal UserDetails userDetails,
      @PathVariable UUID portfolioId,
      @Valid @RequestBody CreatePortfolioRequest request
  ) {
    return ApiResponse.ok(portfolioService.update(userDetails.getUsername(), portfolioId, request));
  }

  @DeleteMapping("/{portfolioId}")
  ResponseEntity<Void> delete(@AuthenticationPrincipal UserDetails userDetails, @PathVariable UUID portfolioId) {
    portfolioService.delete(userDetails.getUsername(), portfolioId);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/{portfolioId}/positions")
  ResponseEntity<ApiResponse<PositionResponse>> addPosition(
      @AuthenticationPrincipal UserDetails userDetails,
      @PathVariable UUID portfolioId,
      @Valid @RequestBody PositionRequest request
  ) {
    return ResponseEntity.status(201).body(ApiResponse.created(portfolioService.addPosition(userDetails.getUsername(), portfolioId, request)));
  }

  @PostMapping("/{portfolioId}/positions/batch")
  ResponseEntity<ApiResponse<List<PositionResponse>>> addPositions(
      @AuthenticationPrincipal UserDetails userDetails,
      @PathVariable UUID portfolioId,
      @Valid @RequestBody List<@Valid PositionRequest> requests
  ) {
    return ResponseEntity.status(201).body(ApiResponse.created(portfolioService.addPositions(userDetails.getUsername(), portfolioId, requests)));
  }

  @PutMapping("/positions/{positionId}")
  ApiResponse<PositionResponse> updatePosition(
      @AuthenticationPrincipal UserDetails userDetails,
      @PathVariable UUID positionId,
      @Valid @RequestBody PositionRequest request
  ) {
    return ApiResponse.ok(portfolioService.updatePosition(userDetails.getUsername(), positionId, request));
  }

  @DeleteMapping("/positions/{positionId}")
  ResponseEntity<Void> deletePosition(@AuthenticationPrincipal UserDetails userDetails, @PathVariable UUID positionId) {
    portfolioService.deletePosition(userDetails.getUsername(), positionId);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/{portfolioId}/analytics")
  ApiResponse<PortfolioAnalyticsResponse> analytics(@AuthenticationPrincipal UserDetails userDetails, @PathVariable UUID portfolioId) {
    return ApiResponse.ok(analyticsService.portfolioAnalytics(userDetails.getUsername(), portfolioId));
  }

  @GetMapping("/{portfolioId}/transactions")
  ApiResponse<List<TransactionResponse>> transactions(@AuthenticationPrincipal UserDetails userDetails, @PathVariable UUID portfolioId) {
    return ApiResponse.ok(transactionService.listForPortfolio(userDetails.getUsername(), portfolioId));
  }

  @PostMapping("/positions/{positionId}/transactions")
  ResponseEntity<ApiResponse<TransactionResponse>> addTransaction(
      @AuthenticationPrincipal UserDetails userDetails,
      @PathVariable UUID positionId,
      @Valid @RequestBody TransactionRequest request
  ) {
    return ResponseEntity.status(201).body(ApiResponse.created(transactionService.add(userDetails.getUsername(), positionId, request)));
  }
}
