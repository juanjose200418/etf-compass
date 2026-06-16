#!/usr/bin/env bash
set -euo pipefail

if [[ -z "${FMP_API_KEY:-}" ]]; then
  echo "FMP_API_KEY is required" >&2
  exit 1
fi

if [[ -z "${JWT_SECRET:-}" ]]; then
  echo "JWT_SECRET is required" >&2
  exit 1
fi

export MARKET_DATA_PROVIDER=fmp

mvn spring-boot:run
