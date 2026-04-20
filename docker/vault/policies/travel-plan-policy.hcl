# Policy for Travel Plan microservices
path "secret/data/travel-plan/*" {
  capabilities = ["read", "list"]
}

path "secret/metadata/travel-plan/*" {
  capabilities = ["read", "list"]
}

# Database credentials
path "database/creds/travel-db-role" {
  capabilities = ["read"]
}

# Transit encryption for sensitive data
path "transit/encrypt/travel-plan" {
  capabilities = ["update"]
}

path "transit/decrypt/travel-plan" {
  capabilities = ["update"]
}
