param(
    [string]$Inventory = "ansible/inventory/hosts.yml",
    [string]$Playbook = "ansible/playbooks/deploy-all.yml",
    [string]$Environment = "staging",
    [string]$ExtraVars = "",
    [string]$OutputDir = "artifacts/ansible",
    [switch]$SkipIdempotenceCheck
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Get-AbsolutePath([string]$Path, [string]$Root) {
    if ([System.IO.Path]::IsPathRooted($Path)) {
        return $Path
    }

    return Join-Path $Root $Path
}

function New-RunDirectory([string]$BaseOutputDir, [string]$Root) {
    $timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
    $absoluteOutputDir = Get-AbsolutePath -Path $BaseOutputDir -Root $Root
    $runDir = Join-Path $absoluteOutputDir $timestamp

    New-Item -ItemType Directory -Path $runDir -Force | Out-Null
    return @{ Timestamp = $timestamp; RunDir = $runDir }
}

function Parse-Recap([string[]]$Lines) {
    $recap = @()
    $pattern = '^(?<host>[^\s]+)\s*:\s*ok=(?<ok>\d+)\s+changed=(?<changed>\d+)\s+unreachable=(?<unreachable>\d+)\s+failed=(?<failed>\d+)\s+skipped=(?<skipped>\d+)\s+rescued=(?<rescued>\d+)\s+ignored=(?<ignored>\d+)'

    foreach ($line in $Lines) {
        $trimmed = $line.Trim()
        if ($trimmed -match $pattern) {
            $recap += [pscustomobject]@{
                host = $Matches.host
                ok = [int]$Matches.ok
                changed = [int]$Matches.changed
                unreachable = [int]$Matches.unreachable
                failed = [int]$Matches.failed
                skipped = [int]$Matches.skipped
                rescued = [int]$Matches.rescued
                ignored = [int]$Matches.ignored
            }
        }
    }

    return $recap
}

function Get-RecapTotals($Recap) {
    $totals = [ordered]@{
        hosts = 0
        ok = 0
        changed = 0
        unreachable = 0
        failed = 0
        skipped = 0
        rescued = 0
        ignored = 0
    }

    if (-not $Recap) {
        return [pscustomobject]$totals
    }

    $totals.hosts = @($Recap).Count
    foreach ($entry in $Recap) {
        $totals.ok += $entry.ok
        $totals.changed += $entry.changed
        $totals.unreachable += $entry.unreachable
        $totals.failed += $entry.failed
        $totals.skipped += $entry.skipped
        $totals.rescued += $entry.rescued
        $totals.ignored += $entry.ignored
    }

    return [pscustomobject]$totals
}

function Run-AnsiblePass {
    param(
        [string]$PassName,
        [string]$RootDir,
        [string]$InventoryPath,
        [string]$PlaybookPath,
        [string]$EnvironmentName,
        [string]$ExtraVarsRaw,
        [string]$LogFile
    )

    $arguments = @(
        "-i", $InventoryPath,
        $PlaybookPath,
        "-e", "environment=$EnvironmentName"
    )

    if (-not [string]::IsNullOrWhiteSpace($ExtraVarsRaw)) {
        $arguments += @("-e", $ExtraVarsRaw)
    }

    Write-Host "[$PassName] Running ansible-playbook..."
    $outputLines = & ansible-playbook @arguments 2>&1
    $outputLines | Tee-Object -FilePath $LogFile | Out-Null
    $exitCode = $LASTEXITCODE

    $recap = Parse-Recap -Lines $outputLines
    $totals = Get-RecapTotals -Recap $recap

    return [pscustomobject]@{
        pass = $PassName
        command = "ansible-playbook " + ($arguments -join " ")
        exitCode = $exitCode
        logFile = $LogFile
        recap = $recap
        totals = $totals
        success = ($exitCode -eq 0 -and $totals.failed -eq 0 -and $totals.unreachable -eq 0)
    }
}

$projectRoot = Split-Path -Parent $PSScriptRoot

if (-not (Get-Command ansible-playbook -ErrorAction SilentlyContinue)) {
    throw "ansible-playbook command not found. Install Ansible or run from an environment where it is available."
}

$inventoryPath = Get-AbsolutePath -Path $Inventory -Root $projectRoot
$playbookPath = Get-AbsolutePath -Path $Playbook -Root $projectRoot

if (-not (Test-Path $inventoryPath)) {
    throw "Inventory file not found: $inventoryPath"
}
if (-not (Test-Path $playbookPath)) {
    throw "Playbook file not found: $playbookPath"
}

$runData = New-RunDirectory -BaseOutputDir $OutputDir -Root $projectRoot
$runDir = $runData.RunDir
$timestamp = $runData.Timestamp

$run1Log = Join-Path $runDir "run-1.log"
$run2Log = Join-Path $runDir "run-2.log"

$run1 = Run-AnsiblePass -PassName "run-1" -RootDir $projectRoot -InventoryPath $inventoryPath -PlaybookPath $playbookPath -EnvironmentName $Environment -ExtraVarsRaw $ExtraVars -LogFile $run1Log

$run2 = $null
$idempotence = $null
if (-not $SkipIdempotenceCheck) {
    $run2 = Run-AnsiblePass -PassName "run-2" -RootDir $projectRoot -InventoryPath $inventoryPath -PlaybookPath $playbookPath -EnvironmentName $Environment -ExtraVarsRaw $ExtraVars -LogFile $run2Log
    $idempotence = ($run2.success -and $run2.totals.changed -eq 0)
}

$summary = [ordered]@{
    generatedAt = (Get-Date).ToString("o")
    environment = $Environment
    inventory = $inventoryPath
    playbook = $playbookPath
    skipIdempotenceCheck = [bool]$SkipIdempotenceCheck
    runDirectory = $runDir
    run1 = $run1
    run2 = $run2
    idempotencePassed = $idempotence
    overallSuccess = ($run1.success -and ($SkipIdempotenceCheck -or $idempotence))
}

$summaryPath = Join-Path $runDir "summary.json"
$summary | ConvertTo-Json -Depth 10 | Set-Content -Path $summaryPath

$reportPath = Join-Path $runDir "report.md"
$run1Changed = if ($run1) { $run1.totals.changed } else { "n/a" }
$run2Changed = if ($run2) { $run2.totals.changed } else { "n/a" }
$run2Status = if ($run2) { if ($idempotence) { "PASS" } else { "FAIL" } } else { "SKIPPED" }

$report = @"
# Ansible Execution Evidence

- Generated at: $($summary.generatedAt)
- Environment: $Environment
- Inventory: $inventoryPath
- Playbook: $playbookPath
- Output directory: $runDir

## Run Results

| Check | Result |
|-------|--------|
| Run 1 successful (no failed/unreachable) | $($run1.success) |
| Run 1 changed tasks | $run1Changed |
| Run 2 idempotence check | $run2Status |
| Run 2 changed tasks | $run2Changed |
| Overall success | $($summary.overallSuccess) |

## Logs

- run-1: $run1Log
- run-2: $run2Log
- summary: $summaryPath
"@

Set-Content -Path $reportPath -Value $report

Write-Host "Ansible evidence generated:"
Write-Host "- $summaryPath"
Write-Host "- $reportPath"

if (-not $summary.overallSuccess) {
    exit 1
}

exit 0