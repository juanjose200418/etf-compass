package com.etfcompass.backend.controller;

import com.etfcompass.backend.dto.ApiResponse;
import com.etfcompass.backend.dto.dashboard.DashboardResponse;
import com.etfcompass.backend.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

  private final AnalyticsService analyticsService;

  @GetMapping
  ApiResponse<DashboardResponse> dashboard(@AuthenticationPrincipal UserDetails userDetails) {
    return ApiResponse.ok(analyticsService.dashboard(userDetails.getUsername()));
  }
}
