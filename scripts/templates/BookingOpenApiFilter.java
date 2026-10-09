package ru.ifmo.soa.booking.config;

import io.swagger.v3.core.filter.AbstractSpecFilter;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Preserves the schema layout of the original Booking Service contract.
 * Swagger Core normally extracts the anonymous violation item into a separate
 * component; the source specification deliberately keeps it inline.
 */
public class BookingOpenApiFilter extends AbstractSpecFilter {

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public Optional<OpenAPI> filterOpenAPI(
            OpenAPI openApi,
            Map<String, List<String>> resourcePathParams,
            Map<String, String> resourceQueryParams,
            Map<String, List<String>> resourceHeaderParams
    ) {
        openApi.setOpenapi("3.0.3");
        openApi.setInfo(new Info()
                .title("Booking Service API")
                .description("Спецификация сервиса управления билетами и сервиса бронирования.\n"
                        + "Все параметры операций передаются в URL. В POST, PUT и PATCH данные Ticket\n"
                        + "передаются в query-параметре `ticket` как JSON.\n"
                        + "Поля id, creationDate и event.id генерируются сервером.\n")
                .version("1.0.0")
                .contact(new Contact().name("Nostya && Dasha")));
        openApi.setServers(List.of(new Server()
                .url("/booking")
                .description("Booking service")));
        openApi.setTags(List.of(new Tag()
                .name("Booking")
                .description("Продажа билетов и отмена бронирований")));

        Operation cancel = openApi.getPaths()
                .get("/person/{person-id}/cancel").getDelete();
        parameter(cancel, "person-id").setDescription("Идентификатор человека");

        Operation sell = openApi.getPaths()
                .get("/sell/{ticket-id}/{person-id}/{price}").getPost();
        parameter(sell, "ticket-id").setDescription(
                "Идентификатор продаваемого билета");
        parameter(sell, "person-id").setDescription(
                "Идентификатор человека, которому продаётся билет");
        parameter(sell, "price").setDescription("Сумма продажи билета");

        Map<String, Schema> schemas = openApi.getComponents().getSchemas();
        Schema errorResponse = schemas.get("ErrorResponse");
        Schema violation = schemas.get("ErrorResponseViolations");

        if (errorResponse == null || violation == null) {
            throw new IllegalStateException(
                    "Expected ErrorResponse and ErrorResponseViolations schemas"
            );
        }

        Schema violations = (Schema) errorResponse.getProperties().get("violations");
        violation.setDescription(null);
        violations.setItems(violation);

        errorResponse.setTitle("Ошибка API");
        errorResponse.setRequired(List.of(
                "timestamp", "status", "message", "error", "path"
        ));
        schemas.remove("ErrorResponseViolations");

        return Optional.of(openApi);
    }

    private io.swagger.v3.oas.models.parameters.Parameter parameter(
            Operation operation,
            String name
    ) {
        return operation.getParameters().stream()
                .filter(parameter -> name.equals(parameter.getName()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Expected OpenAPI parameter: " + name));
    }
}
