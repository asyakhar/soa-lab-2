#!/usr/bin/env bash
set -euo pipefail

echo "Клиент будет доступен по адресу https://localhost:5080"
exec ssh -N \
    -p 2222 \
    -L 5080:127.0.0.1:18230 \
    s409792@helios.cs.ifmo.ru
