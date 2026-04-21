param(
    [string]$PostgresContainer = "docker-postgres-primary-1",
    [string]$DbName = "travel_db",
    [string]$DbUser = "travel_admin",
    [string]$DbPassword = "travel_secret_2024"
)

$healthSql = @"
SELECT
    current_database() AS database_name,
    now() AS checked_at,
    version() AS postgres_version;

SELECT
    n.nspname AS schema_name,
    count(*) AS table_count,
    COALESCE(sum(s.n_live_tup), 0)::bigint AS approx_total_rows
FROM pg_class c
JOIN pg_namespace n ON n.oid = c.relnamespace
LEFT JOIN pg_stat_user_tables s ON s.relid = c.oid
WHERE c.relkind = 'r'
  AND n.nspname IN ('auth_schema', 'user_schema', 'travel_schema', 'payment_schema')
GROUP BY n.nspname
ORDER BY n.nspname;

SELECT
    n.nspname AS schema_name,
    c.relname AS table_name,
    COALESCE(s.n_live_tup, 0)::bigint AS approx_rows,
    pg_size_pretty(pg_total_relation_size(c.oid)) AS total_size,
    COALESCE(to_char(s.last_analyze, 'YYYY-MM-DD HH24:MI:SS'), 'n/a') AS last_analyze
FROM pg_class c
JOIN pg_namespace n ON n.oid = c.relnamespace
LEFT JOIN pg_stat_user_tables s ON s.relid = c.oid
WHERE c.relkind = 'r'
  AND n.nspname IN ('auth_schema', 'user_schema', 'travel_schema', 'payment_schema')
ORDER BY n.nspname, c.relname;
"@

$healthSql | docker exec -i $PostgresContainer env PGPASSWORD=$DbPassword psql -U $DbUser -d $DbName -v ON_ERROR_STOP=1
