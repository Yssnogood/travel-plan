param(
    [string]$Email = "admin@travel-plan.com",
    [string]$FirstName = "System",
    [string]$LastName = "Admin",
    [string]$PasswordHash = "$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrqkQeq6a.TvwWJB9YFHB7c7JKxZHa",
    [string]$PostgresContainer = "docker-postgres-primary-1",
    [string]$DbName = "travel_db",
    [string]$DbUser = "travel_admin",
    [string]$DbPassword = "travel_secret_2024"
)

function Escape-SqlLiteral {
    param([string]$Value)
    return $Value.Replace("'", "''")
}

$emailEscaped = Escape-SqlLiteral $Email
$firstNameEscaped = Escape-SqlLiteral $FirstName
$lastNameEscaped = Escape-SqlLiteral $LastName
$passwordHashEscaped = Escape-SqlLiteral $PasswordHash

$sql = @"
BEGIN;

INSERT INTO auth_schema.roles (name, description)
VALUES ('ADMIN', 'System administrator with full access')
ON CONFLICT (name) DO NOTHING;

WITH admin_role AS (
    SELECT id FROM auth_schema.roles WHERE name = 'ADMIN'
)
INSERT INTO user_schema.users (
    email,
    password_hash,
    first_name,
    last_name,
    status,
    email_verified,
    role_id
)
SELECT
    '$emailEscaped',
    '$passwordHashEscaped',
    '$firstNameEscaped',
    '$lastNameEscaped',
    'ACTIVE',
    TRUE,
    admin_role.id
FROM admin_role
ON CONFLICT (email) DO UPDATE
SET
    role_id = EXCLUDED.role_id,
    status = 'ACTIVE',
    email_verified = TRUE,
    first_name = EXCLUDED.first_name,
    last_name = EXCLUDED.last_name,
    updated_at = CURRENT_TIMESTAMP;

COMMIT;

SELECT
    u.id,
    u.email,
    u.status,
    u.email_verified,
    r.name AS role
FROM user_schema.users u
JOIN auth_schema.roles r ON r.id = u.role_id
WHERE lower(u.email) = lower('$emailEscaped');
"@

$sql | docker exec -i $PostgresContainer env PGPASSWORD=$DbPassword psql -U $DbUser -d $DbName -v ON_ERROR_STOP=1
