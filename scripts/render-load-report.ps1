param(
    [Parameter(Mandatory = $true)]
    [string]$SummaryJsonPath,
    [Parameter(Mandatory = $true)]
    [string]$ReportPath,
    [string]$ScenarioName = "load-test",
    [string]$BaseUrl = "http://host.docker.internal:8080",
    [double]$P95ThresholdMs = 800,
    [double]$ErrorRateThreshold = 0.05,
    [string]$MetadataPath
)

function Get-IsoDate([string]$value) {
    if ([string]::IsNullOrWhiteSpace($value)) {
        return $null
    }

    return [DateTimeOffset]::Parse($value)
}

$summary = Get-Content $SummaryJsonPath -Raw | ConvertFrom-Json
$metadata = $null
if ($MetadataPath -and (Test-Path $MetadataPath)) {
    $metadata = Get-Content $MetadataPath -Raw | ConvertFrom-Json
}

$durationMetric = $summary.metrics.http_req_duration
$failedMetric = $summary.metrics.http_req_failed
$checksMetric = $summary.metrics.checks
$requestMetric = $summary.metrics.http_reqs

$p95 = [double]$durationMetric.'p(95)'
$avg = [double]$durationMetric.avg
$max = [double]$durationMetric.max
$errorRate = [double]$failedMetric.value
$checksRate = [double]$checksMetric.value
$requestCount = [int]$requestMetric.count

$p95Status = if ($p95 -le $P95ThresholdMs) { "PASS" } else { "FAIL" }
$errorStatus = if ($errorRate -le $ErrorRateThreshold) { "PASS" } else { "FAIL" }

$recoverySection = ""
if ($metadata) {
    $outageStart = Get-IsoDate $metadata.outageStart
    $outageEnd = Get-IsoDate $metadata.outageEnd
    $recoveredAt = Get-IsoDate $metadata.recoveredAt

    $recoverySeconds = $null
    if ($outageEnd -and $recoveredAt) {
        $recoverySeconds = [Math]::Round(($recoveredAt - $outageEnd).TotalSeconds, 2)
    }

    $recoveryStatus = if ($recoverySeconds -ne $null -and $metadata.recoveryTargetSeconds -ne $null -and $recoverySeconds -le [double]$metadata.recoveryTargetSeconds) { "PASS" } else { "FAIL" }

    $validationUrl = if ($metadata.validationUrl) { $metadata.validationUrl } else { $metadata.healthUrl }

    $recoverySection = @"
## Failover Events

| Field | Value |
|-------|-------|
| Target service | $($metadata.serviceName) |
| Warmup seconds | $($metadata.warmupSeconds) |
| Outage seconds | $($metadata.outageSeconds) |
| Outage start | $($metadata.outageStart) |
| Outage end | $($metadata.outageEnd) |
| Recovery target (s) | $($metadata.recoveryTargetSeconds) |
| Recovery observed (s) | $recoverySeconds |
| Recovery status | $recoveryStatus |
| Validation URL | $validationUrl |

"@
}

$report = @"
# Load Test Report - $ScenarioName

- Generated at: $(Get-Date -Format s)
- Base URL: $BaseUrl
- Summary source: $SummaryJsonPath

## Acceptance Thresholds

| Metric | Target | Actual | Status |
|--------|--------|--------|--------|
| HTTP p95 latency | <= $P95ThresholdMs ms | $([Math]::Round($p95, 2)) ms | $p95Status |
| HTTP error rate | <= $ErrorRateThreshold | $([Math]::Round($errorRate, 4)) | $errorStatus |
| Check success rate | informational | $([Math]::Round($checksRate, 4)) | INFO |

## Observed Metrics

| Metric | Value |
|--------|-------|
| HTTP requests | $requestCount |
| Average latency | $([Math]::Round($avg, 2)) ms |
| P95 latency | $([Math]::Round($p95, 2)) ms |
| Max latency | $([Math]::Round($max, 2)) ms |
| Error rate | $([Math]::Round($errorRate, 4)) |
| Check success rate | $([Math]::Round($checksRate, 4)) |

$recoverySection## Conclusion

This report is versioned to provide an auditable record of performance and service interruption behavior for the current codebase state.
"@

Set-Content -Path $ReportPath -Value $report