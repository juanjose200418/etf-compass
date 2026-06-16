package com.etfcompass.backend.service;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Locale;

enum EtfHistoryRange {
  MAX,
  Y5,
  Y1,
  M6,
  M3,
  M1,
  W1,
  D1;

  static EtfHistoryRange from(String raw) {
    if (raw == null || raw.isBlank()) {
      return Y1;
    }

    return switch (raw.trim().toUpperCase(Locale.ROOT)) {
      case "MAX" -> MAX;
      case "5Y" -> Y5;
      case "1Y" -> Y1;
      case "6M" -> M6;
      case "3M" -> M3;
      case "1M" -> M1;
      case "1W" -> W1;
      case "1D" -> D1;
      default -> throw new IllegalArgumentException("Unsupported range: " + raw);
    };
  }

  String apiValue() {
    return switch (this) {
      case MAX -> "MAX";
      case Y5 -> "5Y";
      case Y1 -> "1Y";
      case M6 -> "6M";
      case M3 -> "3M";
      case M1 -> "1M";
      case W1 -> "1W";
      case D1 -> "1D";
    };
  }

  LocalDate fromDate(LocalDate today) {
    return switch (this) {
      case MAX -> today.minusYears(30);
      case Y5 -> today.minusYears(5);
      case Y1 -> today.minusYears(1);
      case M6 -> today.minusMonths(6);
      case M3 -> today.minusMonths(3);
      case M1 -> today.minusMonths(1);
      case W1 -> today.minusWeeks(1);
      case D1 -> today.minusDays(2);
    };
  }

  long fromEpochSeconds() {
    return fromDate(LocalDate.now(ZoneOffset.UTC)).atStartOfDay().toEpochSecond(ZoneOffset.UTC);
  }

  String finnhubResolution() {
    return switch (this) {
      case D1 -> "15";
      case W1 -> "60";
      case M1, M3, M6, Y1, Y5, MAX -> "D";
    };
  }
}
