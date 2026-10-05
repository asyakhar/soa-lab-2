#!/usr/bin/env bash
set -euo pipefail

echo "Клиент будет доступен по адресу http://localhost:5080"
exec ssh -N \
    -i "$HOME/.ssh/id_ed25519_helios" \
    -p 2222 \
    -L 5080:127.0.0.1:18230 \
    s408303@helios.cs.ifmo.ru
