# Client application

Простое Flask-приложение для работы с Ticket Service и Booking Service.
Запросы к HTTPS-сервисам идут через Flask, поэтому самоподписанные
сертификаты и ограничения CORS не мешают работе браузера.

Локальный запуск из корня проекта:

```bash
./scripts/start-client-local.sh
```

Адрес: `http://localhost:5000`.

Для других адресов сервисов можно задать `TICKET_SERVICE_URL`,
`BOOKING_SERVICE_URL` и `CLIENT_PORT`.
