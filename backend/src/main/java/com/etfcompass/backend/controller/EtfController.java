package com.etfcompass.backend.controller;

import com.etfcompass.backend.dto.ApiResponse;
import com.etfcompass.backend.dto.etf.EtfCompareResponse;
import com.etfcompass.backend.dto.etf.EtfDetailResponse;
import com.etfcompass.backend.dto.etf.EtfOverlapResponse;
import com.etfcompass.backend.dto.etf.EtfResponse;
import com.etfcompass.backend.service.EtfService;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/etfs")
@RequiredArgsConstructor
public class EtfController {

  private final EtfService etfService;

  @GetMapping
  ApiResponse<List<EtfResponse>> search(@RequestParam(defaultValue = "") @Size(max = 80) String q) {
    return ApiResponse.ok(etfService.search(q));
  }

  @GetMapping("/{ticker}")
  ApiResponse<EtfDetailResponse> get(@PathVariable String ticker) {
    return ApiResponse.ok(etfService.getDetail(ticker));
  }

  @GetMapping("/compare")
  ApiResponse<EtfCompareResponse> compare(@RequestParam List<String> tickers) {
    return ApiResponse.ok(etfService.compare(tickers));
  }

  @GetMapping("/overlap")
  ApiResponse<EtfOverlapResponse> overlap(@RequestParam String left, @RequestParam String right) {
    return ApiResponse.ok(etfService.overlap(left, right));
  }
}
