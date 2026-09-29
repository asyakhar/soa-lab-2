#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LAB2_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
WILDFLY_HOME="$LAB2_DIR/wildfly/wildfly-ee-10-41.0.1.Final"
SERVERS_DIR="$LAB2_DIR/wildfly/servers"

if [[ ! -x "$WILDFLY_HOME/bin/standalone.sh" ]]; then
    echo "WildFly не найден: $WILDFLY_HOME" >&2
    exit 1
fi

create_server() {
    local name="$1"
    local base="$SERVERS_DIR/$name"

    mkdir -p "$base"/{configuration,data,deployments,log,tmp}

    if [[ ! -f "$base/configuration/standalone.xml" ]]; then
        cp -R "$WILDFLY_HOME/standalone/configuration/." "$base/configuration/"

        sed -i '' '/<http-listener name="default"/d' \
            "$base/configuration/standalone.xml"
        sed -i '' \
            's/connector-ref="default"/connector-ref="https"/' \
            "$base/configuration/standalone.xml"
    fi
}

create_server ticket
create_server booking

echo "сервера тут $SERVERS_DIR"
