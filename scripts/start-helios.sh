#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LAB2_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
WILDFLY_HOME="$LAB2_DIR/wildfly/wildfly-ee-10-41.0.1.Final"
SERVERS_DIR="$LAB2_DIR/wildfly/servers"
TLS_DIR="$LAB2_DIR/wildfly/tls"
TICKET_OFFSET=9587
BOOKING_OFFSET=9687
TICKET_PORT=18030
BOOKING_PORT=18130
CLIENT_PORT=18230

COMMON_JAVA_OPTS="-Xms32m -Xss256k -XX:ActiveProcessorCount=2 -XX:+UseSerialGC --add-opens=java.base/java.util=ALL-UNNAMED"
export JAVA_OPTS="$COMMON_JAVA_OPTS -Xmx160m -XX:MaxMetaspaceSize=144m"

wait_for_server() {
    local name="$1"
    local base="$2"
    local pid="$3"

    for _ in {1..90}; do
        if grep -q 'WFLYSRV0025' "$base/log/server.log" 2>/dev/null; then
            echo "$name запущен (PID $pid)."
            return
        fi
        if grep -q 'WFLYSRV0026' "$base/log/server.log" 2>/dev/null; then
            echo "$name запустился с ошибками." >&2
            tail -60 "$base/log/server.log" >&2
            exit 1
        fi
        if ! kill -0 "$pid" 2>/dev/null; then
            echo "$name завершился с ошибкой." >&2
            tail -60 "$base/log/console.log" >&2
            exit 1
        fi
        sleep 1
    done

    echo "$name не успел запуститься." >&2
    exit 1
}

start_wildfly() {
    local name="$1"
    local base="$2"
    local offset="$3"
    shift 3

    : >"$base/log/server.log"
    : >"$base/log/console.log"
    nohup "$WILDFLY_HOME/bin/standalone.sh" \
        -Djboss.server.base.dir="$base" \
        -Djboss.socket.binding.port-offset="$offset" \
        -Djboss.bind.address=127.0.0.1 \
        -Djboss.bind.address.management=127.0.0.1 \
        "$@" >"$base/log/console.log" 2>&1 &

    local pid=$!
    echo "$pid" >"$base/server.pid"
    wait_for_server "$name" "$base" "$pid"
}

start_wildfly "Ticket WildFly" "$SERVERS_DIR/ticket" "$TICKET_OFFSET" \
    -Djboss.tx.node.id=ticket

mkdir -p "$TLS_DIR"
curl -sk "https://127.0.0.1:$TICKET_PORT/" >/dev/null
keytool -J-Xmx128m -exportcert -rfc -alias server \
    -keystore "$SERVERS_DIR/ticket/configuration/application.keystore" \
    -storepass password -file "$TLS_DIR/ticket-service.crt" >/dev/null

rm -f "$TLS_DIR/booking-truststore.p12"
keytool -J-Xmx128m -importcert -noprompt -alias ticket-service \
    -file "$TLS_DIR/ticket-service.crt" \
    -keystore "$TLS_DIR/booking-truststore.p12" \
    -storetype PKCS12 -storepass changeit >/dev/null

export JAVA_OPTS="$COMMON_JAVA_OPTS -Xmx128m -XX:MaxMetaspaceSize=128m"
start_wildfly "Booking WildFly" "$SERVERS_DIR/booking" "$BOOKING_OFFSET" \
    -Djboss.tx.node.id=booking \
    -Dticket.service.url="https://localhost:$TICKET_PORT/api/v1" \
    -Djavax.net.ssl.trustStore="$TLS_DIR/booking-truststore.p12" \
    -Djavax.net.ssl.trustStorePassword=changeit

CLIENT_DIR="$LAB2_DIR/client"
if [[ ! -x "$CLIENT_DIR/.venv/bin/python" ]]; then
    python3.11 -m venv "$CLIENT_DIR/.venv"
fi
"$CLIENT_DIR/.venv/bin/pip" install --quiet -r "$CLIENT_DIR/requirements.txt"

nohup env \
    TICKET_SERVICE_URL="https://127.0.0.1:$TICKET_PORT/api/v1" \
    BOOKING_SERVICE_URL="https://127.0.0.1:$BOOKING_PORT/booking" \
    CLIENT_PORT="$CLIENT_PORT" \
    "$CLIENT_DIR/.venv/bin/python" "$CLIENT_DIR/app.py" \
    >"$CLIENT_DIR/client.log" 2>&1 &
echo $! >"$CLIENT_DIR/client.pid"

echo "Ticket Service:  https://127.0.0.1:$TICKET_PORT/api/v1"
echo "Booking Service: https://127.0.0.1:$BOOKING_PORT/booking"
echo "Client:          http://127.0.0.1:$CLIENT_PORT"
