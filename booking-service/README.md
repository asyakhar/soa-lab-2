# Booking Service

JAX-RS service generated from `../specs/booking-service.openapi.json` and
adapted for Jakarta EE 10 and deployment to WildFly.

Build and test:

```bash
./gradlew clean check war
```

The WAR file is written to `build/libs/booking.war`.

## Verify OpenAPI contract

Generate an OpenAPI document from the JAX-RS and Swagger annotations and compare
its operations, parameters, responses and component schemas with
`../specs/booking-service.openapi.json`:

```bash
./gradlew verifyOpenApiContract
```

The generated document is written to
`build/openapi/booking-service.generated.json`. The regular `check` task also
runs this verification automatically.
