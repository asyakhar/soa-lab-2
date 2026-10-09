#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LAB2_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
TLS_DIR="$LAB2_DIR/wildfly/tls"
CERT_FILE="$TLS_DIR/client-cert.pem"
KEY_FILE="$TLS_DIR/client-key.pem"

mkdir -p "$TLS_DIR"

if [[ -f "$CERT_FILE" && -f "$KEY_FILE" ]]; then
    echo "HTTPS-сертификат клиента уже существует."
    exit 0
fi

openssl req -x509 -newkey rsa:2048 -sha256 -nodes \
    -days 3650 \
    -subj "/CN=localhost" \
    -addext "subjectAltName=DNS:localhost,IP:127.0.0.1" \
    -keyout "$KEY_FILE" \
    -out "$CERT_FILE" \
    >/dev/null 2>&1

chmod 600 "$KEY_FILE"
echo "HTTPS-сертификат клиента создан в $TLS_DIR."
