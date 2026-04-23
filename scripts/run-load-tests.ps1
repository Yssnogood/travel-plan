param(
    [string]$Scenario = "admin-payments-baseline.js",
    [ValidateSet("capacity", "protection")]
    [string]$Mode = "capacity",
    [string]$BaseUrl = "http://host.docker.internal:8080",
    [string]$AdminEmail = "admin@travel-plan.com",
    [string]$AdminPassword = "admin123",
    [string]$Vus = "10",
    [string]$Duration = "60s",
    [string]$P95Threshold = "800",
    [string]$ErrorRateThreshold = "0.05"
)

$projectRoot = Split-Path -Parent $PSScriptRoot
$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$scenarioPath = "/workspace/tests/load/k6/$Scenario"
$summaryPath = "/workspace/docs/reports/load/$timestamp-baseline-summary.json"
$reportPath = Join-Path $projectRoot "docs\reports\load\$timestamp-baseline-report.md"
$envFile = Join-Path $projectRoot ".env"
$jwtSecret = $null

if (Test-Path $envFile) {
    $jwtSecretLine = Get-Content $envFile | Where-Object { $_ -match '^JWT_SECRET=' } | Select-Object -First 1
    if ($jwtSecretLine) {
        $jwtSecret = ($jwtSecretLine -split '=', 2)[1].Trim()
    }
}

docker run --rm `
    -v "${projectRoot}:/workspace" `
    -w /workspace `
    -e BASE_URL="$BaseUrl" `
    -e ADMIN_EMAIL="$AdminEmail" `
    -e ADMIN_PASSWORD="$AdminPassword" `
    -e JWT_SECRET="$jwtSecret" `
    -e K6_MODE="$Mode" `
    -e K6_VUS="$Vus" `
    -e K6_DURATION="$Duration" `
    -e K6_P95_THRESHOLD="$P95Threshold" `
    -e K6_ERROR_RATE_THRESHOLD="$ErrorRateThreshold" `
    grafana/k6:0.49.0 run --summary-export "$summaryPath" "$scenarioPath"

& "$PSScriptRoot\render-load-report.ps1" `
    -SummaryJsonPath (Join-Path $projectRoot "docs\reports\load\$timestamp-baseline-summary.json") `
    -ReportPath $reportPath `
    -ScenarioName "baseline-admin-payments-$Mode" `
    -BaseUrl $BaseUrl `
    -P95ThresholdMs ([double]$P95Threshold) `
    -ErrorRateThreshold ([double]$ErrorRateThreshold)

Write-Host "Load test summary written to docs/reports/load/$timestamp-baseline-summary.json"
Write-Host "Load test report written to docs/reports/load/$timestamp-baseline-report.md"