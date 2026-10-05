#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LAB2_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
WILDFLY_HOME="$LAB2_DIR/wildfly/wildfly-ee-10-41.0.1.Final"
export JAVA_OPTS="-Xms16m -Xmx96m -XX:ActiveProcessorCount=1 -XX:+UseSerialGC"

if [[ -f "$LAB2_DIR/client/client.pid" ]]; then
    kill "$(cat "$LAB2_DIR/client/client.pid")" 2>/dev/null || true
fi

"$WILDFLY_HOME/bin/jboss-cli.sh" --connect \
    --controller=127.0.0.1:19677 --command=':shutdown' >/dev/null 2>&1 || true
"$WILDFLY_HOME/bin/jboss-cli.sh" --connect \
    --controller=127.0.0.1:19577 --command=':shutdown' >/dev/null 2>&1 || true

echo "Сервисы Helios остановлены."
