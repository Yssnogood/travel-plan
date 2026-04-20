#!/bin/bash
# SSL Certificate Generation Script for Travel Plan
# Usage: ./generate-certs.sh [environment]

set -e

ENVIRONMENT=${1:-development}
CERT_DIR="./certs"
DAYS_VALID=365

echo "Generating SSL certificates for environment: $ENVIRONMENT"

# Create cert directory
mkdir -p "$CERT_DIR"

# Generate CA private key and certificate
echo "Generating CA certificate..."
openssl genrsa -out "$CERT_DIR/ca.key" 4096
openssl req -x509 -new -nodes -key "$CERT_DIR/ca.key" \
    -sha256 -days $DAYS_VALID \
    -out "$CERT_DIR/ca.crt" \
    -subj "/C=FR/ST=IDF/L=Paris/O=TravelPlan/OU=IT/CN=TravelPlan-CA"

# Generate server private key
echo "Generating server certificate..."
openssl genrsa -out "$CERT_DIR/server.key" 2048

# Generate server CSR
openssl req -new -key "$CERT_DIR/server.key" \
    -out "$CERT_DIR/server.csr" \
    -subj "/C=FR/ST=IDF/L=Paris/O=TravelPlan/OU=IT/CN=*.travelplan.com"

# Create SAN config
cat > "$CERT_DIR/san.cnf" << EOF
[req]
distinguished_name = req_distinguished_name
req_extensions = v3_req
prompt = no

[req_distinguished_name]
C = FR
ST = IDF
L = Paris
O = TravelPlan
OU = IT
CN = *.travelplan.com

[v3_req]
keyUsage = keyEncipherment, dataEncipherment
extendedKeyUsage = serverAuth
subjectAltName = @alt_names

[alt_names]
DNS.1 = localhost
DNS.2 = *.travelplan.com
DNS.3 = api.travelplan.com
DNS.4 = admin.travelplan.com
DNS.5 = kong
DNS.6 = auth-service
DNS.7 = user-service
DNS.8 = travel-service
DNS.9 = payment-service
DNS.10 = notification-service
IP.1 = 127.0.0.1
EOF

# Sign server certificate with CA
openssl x509 -req -in "$CERT_DIR/server.csr" \
    -CA "$CERT_DIR/ca.crt" -CAkey "$CERT_DIR/ca.key" \
    -CAcreateserial -out "$CERT_DIR/server.crt" \
    -days $DAYS_VALID -sha256 \
    -extfile "$CERT_DIR/san.cnf" -extensions v3_req

# Generate Vault certificate
echo "Generating Vault certificate..."
openssl genrsa -out "$CERT_DIR/vault.key" 2048
openssl req -new -key "$CERT_DIR/vault.key" \
    -out "$CERT_DIR/vault.csr" \
    -subj "/C=FR/ST=IDF/L=Paris/O=TravelPlan/OU=IT/CN=vault"

cat > "$CERT_DIR/vault-san.cnf" << EOF
[v3_req]
keyUsage = keyEncipherment, dataEncipherment
extendedKeyUsage = serverAuth
subjectAltName = @alt_names

[alt_names]
DNS.1 = vault
DNS.2 = localhost
IP.1 = 127.0.0.1
EOF

openssl x509 -req -in "$CERT_DIR/vault.csr" \
    -CA "$CERT_DIR/ca.crt" -CAkey "$CERT_DIR/ca.key" \
    -CAcreateserial -out "$CERT_DIR/vault.crt" \
    -days $DAYS_VALID -sha256 \
    -extfile "$CERT_DIR/vault-san.cnf" -extensions v3_req

# Create Java keystore (for Spring Boot services)
echo "Creating Java keystore..."
openssl pkcs12 -export -in "$CERT_DIR/server.crt" \
    -inkey "$CERT_DIR/server.key" \
    -out "$CERT_DIR/keystore.p12" \
    -name travelplan \
    -password pass:changeit \
    -CAfile "$CERT_DIR/ca.crt" -caname root

# Create truststore with CA certificate
keytool -import -trustcacerts -noprompt \
    -alias travelplan-ca \
    -file "$CERT_DIR/ca.crt" \
    -keystore "$CERT_DIR/truststore.jks" \
    -storepass changeit 2>/dev/null || true

# Set permissions
chmod 600 "$CERT_DIR"/*.key
chmod 644 "$CERT_DIR"/*.crt "$CERT_DIR"/*.p12 "$CERT_DIR"/*.jks 2>/dev/null || true

echo ""
echo "✅ Certificates generated successfully in $CERT_DIR/"
echo ""
echo "Files created:"
ls -la "$CERT_DIR/"
