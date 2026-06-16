#!/usr/bin/env bash
set -euo pipefail

if [[ -z "${FINNHUB_API_KEY:-}" ]]; then
  echo "FINNHUB_API_KEY is required" >&2
  exit 1
fi

if [[ -z "${JWT_SECRET:-}" ]]; then
  echo "JWT_SECRET is required" >&2
  exit 1
fi

export MARKET_DATA_PROVIDER=finnhub

mvn spring-boot:run
