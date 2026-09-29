#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LAB2_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

mvn -q -f "$LAB2_DIR/ticket-service/pom.xml" -DskipTests package
mvn -q -f "$LAB2_DIR/booking-service/pom.xml" -DskipTests package

echo "Built ticket-service and booking-service WAR files."
