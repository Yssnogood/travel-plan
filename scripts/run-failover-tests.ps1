param(
    [string]$Scenario = "admin-payments-baseline.js",
    [string]$BaseUrl = "http://host.docker.internal:8080",
    [string]$ServiceName = "payment-service",
    [int]$WarmupSeconds = 15,
    [int]$OutageSeconds = 10,
    [int]$RecoveryTargetSeconds = 60,
    [int]$Vus = 4,
    [int]$PostRecoverySeconds = 20,
    [double]$P95ThresholdMs = 800,
    [double]$ErrorRateThreshold = 0.25,
    [string]$ValidationBaseUrl = "http://localhost:8080"
)

function ConvertTo-Base64Url {
    param([byte[]]$Bytes)

    return [Convert]::ToBase64String($Bytes).TrimEnd('=') -replace '\+', '-' -replace '/', '_'
}

function New-AdminJwtToken {
    param(
        [string]$Secret,
        [string]$Email
    )

    $now = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
    $headerJson = '{"alg":"HS256","typ":"JWT"}'
    $payloadObj = @{
        userId = 1
        email = $Email
        firstName = 'System'
        lastName = 'Admin'
        role = 'ADMIN'
        permissions = @('admin:access', 'payments:read', 'payments:write')
        sub = $Email
        iat = $now
        exp = $now + 3600
    }

    $payloadJson = $payloadObj | ConvertTo-Json -Compress
    $encodedHeader = ConvertTo-Base64Url([Text.Encoding]::UTF8.GetBytes($headerJson))
    $encodedPayload = ConvertTo-Base64Url([Text.Encoding]::UTF8.GetBytes($payloadJson))
    $unsignedToken = "$encodedHeader.$encodedPayload"
    $hmac = [System.Security.Cryptography.HMACSHA256]::new([Text.Encoding]::UTF8.GetBytes($Secret))
    $signature = ConvertTo-Base64Url($hmac.ComputeHash([Text.Encoding]::UTF8.GetBytes($unsignedToken)))

    return "$unsignedToken.$signature"
}

$projectRoot = Split-Path -Parent $PSScriptRoot
$dockerRoot = Join-Path $projectRoot "docker"
$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$totalDurationSeconds = $WarmupSeconds + $OutageSeconds + $PostRecoverySeconds
$summaryFile = Join-Path $projectRoot "docs\reports\load\$timestamp-failover-summary.json"
$reportFile = Join-Path $projectRoot "docs\reports\load\$timestamp-failover-report.md"
$metadataFile = Join-Path $projectRoot "docs\reports\load\$timestamp-failover-metadata.json"
$k6ContainerName = "travel-plan-k6-failover-$timestamp"
$envFile = Join-Path $projectRoot ".env"
$jwtSecret = $null

if (Test-Path $envFile) {
    $jwtSecretLine = Get-Content $envFile | Where-Object { $_ -match '^JWT_SECRET=' } | Select-Object -First 1
    if ($jwtSecretLine) {
        $jwtSecret = ($jwtSecretLine -split '=', 2)[1].Trim()
    }
}

$scenarioPath = "/workspace/tests/load/k6/$Scenario"
$summaryPath = "/workspace/docs/reports/load/$timestamp-failover-summary.json"
$validationUrl = "$($ValidationBaseUrl.TrimEnd('/'))/api/v1/payments?page=0&size=1"
$validationToken = New-AdminJwtToken -Secret $jwtSecret -Email "admin@travel-plan.com"

$runCommand = @(
    "run",
    "-d",
    "--name", $k6ContainerName,
    "-v", "${projectRoot}:/workspace",
    "-w", "/workspace",
    "-e", "BASE_URL=$BaseUrl",
    "-e", "JWT_SECRET=$jwtSecret",
    "-e", "K6_VUS=$Vus",
    "-e", "K6_DURATION=${totalDurationSeconds}s",
    "-e", "K6_P95_THRESHOLD=$P95ThresholdMs",
    "-e", "K6_ERROR_RATE_THRESHOLD=$ErrorRateThreshold",
    "grafana/k6:0.49.0",
    "run",
    "--summary-export", $summaryPath,
    $scenarioPath
)

$containerId = docker @runCommand
if (-not $containerId) {
    throw "Unable to start k6 failover container"
}

Start-Sleep -Seconds $WarmupSeconds
$outageStart = [DateTimeOffset]::UtcNow

Push-Location $dockerRoot
try {
    docker compose --env-file ..\.env -f docker-compose.infra.yml -f docker-compose.services.yml stop $ServiceName | Out-Null
    Start-Sleep -Seconds $OutageSeconds
    docker compose --env-file ..\.env -f docker-compose.infra.yml -f docker-compose.services.yml up -d $ServiceName | Out-Null
} finally {
    Pop-Location
}

$outageEnd = [DateTimeOffset]::UtcNow
$deadline = (Get-Date).AddSeconds($RecoveryTargetSeconds)
$recoveredAt = $null

while ((Get-Date) -lt $deadline) {
    try {
        $response = Invoke-WebRequest -UseBasicParsing -TimeoutSec 5 -Uri $validationUrl -Headers @{ Authorization = "Bearer $validationToken" }
        if ([int]$response.StatusCode -eq 200) {
            $recoveredAt = [DateTimeOffset]::UtcNow
            break
        }
    } catch {
    }

    Start-Sleep -Seconds 2
}

docker wait $k6ContainerName | Out-Null
docker logs $k6ContainerName | Out-Null
docker rm $k6ContainerName | Out-Null

$metadata = [pscustomobject]@{
    serviceName = $ServiceName
    warmupSeconds = $WarmupSeconds
    outageSeconds = $OutageSeconds
    outageStart = $outageStart.ToString('o')
    outageEnd = $outageEnd.ToString('o')
    recoveredAt = if ($recoveredAt) { $recoveredAt.ToString('o') } else { $null }
    recoveryTargetSeconds = $RecoveryTargetSeconds
    validationUrl = $validationUrl
}

$metadata | ConvertTo-Json | Set-Content -Path $metadataFile

& "$PSScriptRoot\render-load-report.ps1" `
    -SummaryJsonPath $summaryFile `
    -ReportPath $reportFile `
    -ScenarioName "payment-service-failover" `
    -BaseUrl $BaseUrl `
    -P95ThresholdMs $P95ThresholdMs `
    -ErrorRateThreshold $ErrorRateThreshold `
    -MetadataPath $metadataFile

Write-Host "Failover summary written to $summaryFile"
Write-Host "Failover report written to $reportFile"
Write-Host "Failover metadata written to $metadataFile"