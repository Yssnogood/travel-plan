#!/bin/bash

set -euo pipefail

SCENARIO=${1:-admin-payments-baseline.js}
BASE_URL=${BASE_URL:-http://host.docker.internal:8080}
ADMIN_EMAIL=${ADMIN_EMAIL:-admin@travel-plan.com}
ADMIN_PASSWORD=${ADMIN_PASSWORD:-admin123}
K6_VUS=${K6_VUS:-10}
K6_DURATION=${K6_DURATION:-60s}
K6_P95_THRESHOLD=${K6_P95_THRESHOLD:-800}
K6_ERROR_RATE_THRESHOLD=${K6_ERROR_RATE_THRESHOLD:-0.05}

PROJECT_ROOT=$(cd "$(dirname "$0")/.." && pwd)
TIMESTAMP=$(date +%Y%m%d-%H%M%S)
SCENARIO_PATH="/workspace/tests/load/k6/$SCENARIO"
SUMMARY_PATH="/workspace/docs/reports/load/${TIMESTAMP}-baseline-summary.json"
JWT_SECRET=${JWT_SECRET:-$(grep '^JWT_SECRET=' "$PROJECT_ROOT/.env" 2>/dev/null | head -n 1 | cut -d= -f2-)}

docker run --rm \
  -v "$PROJECT_ROOT:/workspace" \
  -w /workspace \
  -e BASE_URL="$BASE_URL" \
  -e ADMIN_EMAIL="$ADMIN_EMAIL" \
  -e ADMIN_PASSWORD="$ADMIN_PASSWORD" \
  -e JWT_SECRET="$JWT_SECRET" \
  -e K6_VUS="$K6_VUS" \
  -e K6_DURATION="$K6_DURATION" \
  -e K6_P95_THRESHOLD="$K6_P95_THRESHOLD" \
  -e K6_ERROR_RATE_THRESHOLD="$K6_ERROR_RATE_THRESHOLD" \
  grafana/k6:0.49.0 run --summary-export "$SUMMARY_PATH" "$SCENARIO_PATH"

echo "Load test summary written to docs/reports/load/${TIMESTAMP}-baseline-summary.json"