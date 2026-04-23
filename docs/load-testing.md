# Load and Failover Testing

This repository includes reproducible load and service interruption scenarios designed for audit evidence.

## Standardized Scenarios

### 1. Baseline load

- Target: `GET /api/v1/payments?page=0&size=10`
- Access: admin JWT generated from local `.env` `JWT_SECRET`
- Tooling: `k6` executed in Docker for consistent runner behavior
- Default thresholds:
  - p95 latency <= 800 ms
  - error rate <= 5%

Run on Windows:

```powershell
.\scripts\run-load-tests.ps1 -Vus 10 -Duration 60s
```

Run on Unix:

```bash
./scripts/run-load-tests.sh admin-payments-baseline.js
```

### 2. Service interruption / failover rehearsal

- Target service: `payment-service`
- Traffic: same authenticated admin payment listing scenario kept under load during the event
- Failure injection: `docker compose stop payment-service`
- Recovery action: `docker compose up -d payment-service`
- Recovery validation: authenticated `GET /api/v1/payments?page=0&size=1` through the gateway

Run on Windows:

```powershell
.\scripts\run-failover-tests.ps1 -WarmupSeconds 15 -OutageSeconds 10 -Vus 4
```

## Versioned Evidence

Generated artifacts are written under `docs/reports/load/`:

- `*-baseline-summary.json`
- `*-baseline-report.md`
- `*-failover-summary.json`
- `*-failover-metadata.json`
- `*-failover-report.md`

These files are intended to be committed when a test campaign is used as audit evidence.

## Interpretation Guidance

- A failed threshold is a valid result; it means the environment did not meet the acceptance target.
- On the current single-replica topology, the failover scenario should be interpreted as a controlled service-loss and recovery rehearsal rather than zero-downtime failover.
- To claim true failover, the architecture must add redundant service instances and routing that survives loss of one replica.