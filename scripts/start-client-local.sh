#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LAB2_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
CLIENT_DIR="$LAB2_DIR/client"
VENV_DIR="$CLIENT_DIR/.venv"

if ! command -v python3 >/dev/null 2>&1; then
    echo "Python 3 не найден." >&2
    exit 1
fi

if [[ ! -x "$VENV_DIR/bin/python" ]]; then
    echo "Создание виртуального окружения клиента..."
    python3 -m venv "$VENV_DIR"
fi

echo "Установка зависимостей клиента..."
"$VENV_DIR/bin/pip" install --quiet -r "$CLIENT_DIR/requirements.txt"

echo "Клиент доступен по адресу http://localhost:${CLIENT_PORT:-5000}"
exec "$VENV_DIR/bin/python" "$CLIENT_DIR/app.py"
