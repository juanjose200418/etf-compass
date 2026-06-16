package com.etfcompass.backend.domain;

import com.fasterxml.jackson.annotation.JsonValue;

public enum DistributionPolicy {
  ACCUMULATING("Accumulating"),
  DISTRIBUTING("Distributing");

  private final String label;

  DistributionPolicy(String label) {
    this.label = label;
  }

  @JsonValue
  public String getLabel() {
    return label;
  }
}
