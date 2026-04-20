storage "file" {
  path = "/vault/data"
}

listener "tcp" {
  address     = "0.0.0.0:8200"
  tls_disable = 0
  tls_cert_file = "/vault/certs/vault.crt"
  tls_key_file  = "/vault/certs/vault.key"
}

api_addr = "https://0.0.0.0:8200"
cluster_addr = "https://0.0.0.0:8201"

ui = true

disable_mlock = true

# Audit logging
log_level = "info"

# Auto-unseal using transit (for production)
# seal "transit" {
#   address = "https://vault-transit:8200"
#   token = "s.xxx"
#   disable_renewal = "false"
#   key_name = "autounseal"
#   mount_path = "transit/"
# }
