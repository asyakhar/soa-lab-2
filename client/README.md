# Client application

Простое Flask-приложение для работы с Ticket Service и Booking Service.
Запросы к HTTPS-сервисам идут через Flask, поэтому самоподписанные
сертификаты и ограничения CORS не мешают работе браузера.

Интерфейс разделён на три микрофронтенда на нативных ES-модулях и Web Components:

- `static/mfe/shell` — общая оболочка, сообщения и цирковые эффекты;
- `static/mfe/ticket` — афиша и все операции Ticket Service;
- `static/mfe/booking` — продажа, отмена и печать билета.

Микрофронтенды общаются через браузерные `CustomEvent` и могут обновляться независимо.

Локальный запуск из корня проекта:

```bash
./scripts/start-client-local.sh
```

Адрес: `https://localhost:5000`. При первом открытии браузер попросит
подтвердить самоподписанный сертификат.

Для других адресов сервисов можно задать `TICKET_SERVICE_URL`,
`BOOKING_SERVICE_URL` и `CLIENT_PORT`.
