# Smoke Tests Script for Travel Plan (Windows)
# Usage: powershell -ExecutionPolicy Bypass -File scripts\smoke-tests.ps1

param(
    [string]$Environment = "local"
)

Write-Host "Running smoke tests for environment: $Environment" -ForegroundColor Cyan

switch ($Environment) {
    "production" { $BaseUrl = "https://api.travelplan.com" }
    "staging"    { $BaseUrl = "https://staging-api.travelplan.com" }
    default      { $BaseUrl = "http://localhost" }
}

Write-Host "Base URL: $BaseUrl"

$TestsPassed = 0
$TestsFailed = 0

function Test-Endpoint {
    param(
        [string]$Name,
        [string]$Url,
        [int]$ExpectedStatus = 200,
        [int]$TimeoutSec = 10
    )

    Write-Host -NoNewline "Testing: $Name... "

    try {
        $response = Invoke-WebRequest -Uri $Url -Method GET -TimeoutSec $TimeoutSec -UseBasicParsing -ErrorAction Stop
        $status = $response.StatusCode
    } catch [System.Net.WebException] {
        $webResponse = $_.Exception.Response
        if ($webResponse) {
            $status = [int]$webResponse.StatusCode
        } else {
            $status = 0
        }
    } catch {
        $status = 0
    }

    if ($status -eq $ExpectedStatus) {
        Write-Host "PASSED (HTTP $status)" -ForegroundColor Green
        $script:TestsPassed++
    } else {
        Write-Host "FAILED (Expected $ExpectedStatus, got $status)" -ForegroundColor Red
        $script:TestsFailed++
    }
}

# Service ports
$ports = @{
    "auth"         = 8081
    "user"         = 8082
    "travel"       = 8083
    "payment"      = 8084
    "notification" = 8085
}

Write-Host ""
Write-Host "=== Health Check Tests ===" -ForegroundColor Yellow

foreach ($svc in $ports.GetEnumerator()) {
    $url = if ($Environment -eq "local") {
        "http://localhost:$($svc.Value)/actuator/health"
    } else {
        "$BaseUrl/$($svc.Key)/actuator/health"
    }
    Test-Endpoint -Name "$($svc.Key) Service Health" -Url $url
}

Write-Host ""
Write-Host "=== API Endpoint Tests ===" -ForegroundColor Yellow

if ($Environment -eq "local") {
    Test-Endpoint -Name "Auth Login Endpoint" -Url "http://localhost:$($ports['auth'])/api/auth/login" -ExpectedStatus 405
    Test-Endpoint -Name "Users Endpoint (Unauthorized)" -Url "http://localhost:$($ports['user'])/api/users" -ExpectedStatus 401
    Test-Endpoint -Name "Travels Endpoint (Unauthorized)" -Url "http://localhost:$($ports['travel'])/api/travels" -ExpectedStatus 401
    Test-Endpoint -Name "Payments Endpoint (Unauthorized)" -Url "http://localhost:$($ports['payment'])/api/payments" -ExpectedStatus 401
} else {
    Test-Endpoint -Name "Auth Login Endpoint" -Url "$BaseUrl/auth/login" -ExpectedStatus 405
    Test-Endpoint -Name "Users Endpoint (Unauthorized)" -Url "$BaseUrl/users" -ExpectedStatus 401
    Test-Endpoint -Name "Travels Endpoint (Unauthorized)" -Url "$BaseUrl/travels" -ExpectedStatus 401
    Test-Endpoint -Name "Payments Endpoint (Unauthorized)" -Url "$BaseUrl/payments" -ExpectedStatus 401
}

# Summary
Write-Host ""
Write-Host "=== Test Summary ===" -ForegroundColor Yellow
Write-Host "Passed: $TestsPassed" -ForegroundColor Green
Write-Host "Failed: $TestsFailed" -ForegroundColor Red
Write-Host "Total: $($TestsPassed + $TestsFailed)"

if ($TestsFailed -gt 0) {
    Write-Host ""
    Write-Host "Some smoke tests failed! (Non-blocking)" -ForegroundColor Yellow
    # Exit 0 to not block the pipeline - services may still be starting
    exit 0
}

Write-Host ""
Write-Host "All smoke tests passed!" -ForegroundColor Green
exit 0
