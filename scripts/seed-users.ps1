param(
    [string]$SeedFile = "scripts/seeds/users.seed.csv",
    [string]$AuthBaseUrl = "http://localhost:8081/api/v1/auth",
    [string]$PostgresContainer = "docker-postgres-primary-1",
    [string]$DbName = "travel_db",
    [string]$DbUser = "travel_admin",
    [string]$DbPassword = "travel_secret_2024"
)

if (-not (Test-Path $SeedFile)) {
    throw "Seed file not found: $SeedFile"
}

function Escape-SqlLiteral {
    param([string]$Value)
    return $Value.Replace("'", "''")
}

$users = Import-Csv -Path $SeedFile
if (-not $users -or $users.Count -eq 0) {
    throw "No users found in seed file: $SeedFile"
}

foreach ($user in $users) {
    if (-not $user.email -or -not $user.password -or -not $user.firstName -or -not $user.lastName -or -not $user.role) {
        Write-Warning "Skipping invalid row (missing required fields): $($user | ConvertTo-Json -Compress)"
        continue
    }

    $payload = @{
        email = $user.email
        password = $user.password
        firstName = $user.firstName
        lastName = $user.lastName
        phone = $user.phone
    } | ConvertTo-Json -Compress

    try {
        Invoke-RestMethod -Method Post -Uri "$AuthBaseUrl/register" -ContentType "application/json" -Body $payload | Out-Null
        Write-Output "Registered user: $($user.email)"
    } catch {
        $statusCode = $null
        if ($_.Exception.Response -and $_.Exception.Response.StatusCode) {
            $statusCode = [int]$_.Exception.Response.StatusCode
        }
        if ($statusCode -eq 409) {
            Write-Output "User already exists: $($user.email)"
        } else {
            throw "Failed to register user $($user.email): $($_.Exception.Message)"
        }
    }

    $emailEscaped = Escape-SqlLiteral $user.email
    $roleEscaped = Escape-SqlLiteral $user.role.ToUpper()

    $sql = @"
UPDATE user_schema.users u
SET role_id = r.id,
    status = 'ACTIVE',
    email_verified = TRUE,
    updated_at = CURRENT_TIMESTAMP
FROM auth_schema.roles r
WHERE lower(u.email) = lower('$emailEscaped')
  AND upper(r.name) = '$roleEscaped';
"@

    $sql | docker exec -i $PostgresContainer env PGPASSWORD=$DbPassword psql -U $DbUser -d $DbName -v ON_ERROR_STOP=1 | Out-Null
    Write-Output "Ensured role/status for: $($user.email) -> $($user.role.ToUpper())"
}

$summarySql = @"
SELECT u.email, u.status, u.email_verified, r.name AS role
FROM user_schema.users u
JOIN auth_schema.roles r ON r.id = u.role_id
WHERE lower(u.email) IN (
$(($users | ForEach-Object { "    lower('" + (Escape-SqlLiteral $_.email) + "')" }) -join ",`n")
)
ORDER BY u.email;
"@

$summarySql | docker exec -i $PostgresContainer env PGPASSWORD=$DbPassword psql -U $DbUser -d $DbName -v ON_ERROR_STOP=1
