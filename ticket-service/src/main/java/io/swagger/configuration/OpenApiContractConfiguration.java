package io.swagger.configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Configuration
public class OpenApiContractConfiguration {

    @Bean
    public OpenApiCustomizer ticketContractCustomizer() {
        return openApi -> {
            applyMetadata(openApi);
            applySchemas(openApi);
            applyParameters(openApi);
            applyResponseHeaders(openApi);
        };
    }

    private void applyMetadata(OpenAPI openApi) {
        openApi.setOpenapi("3.0.3");
        openApi.setInfo(new Info()
                .title("Ticket Service API")
                .description("Спецификация сервиса управления билетами и сервиса бронирования.\n"
                        + "Все параметры операций передаются в URL. В POST, PUT и PATCH данные Ticket\n"
                        + "передаются в query-параметре `ticket` как JSON.\n"
                        + "Поля id, creationDate и event.id генерируются сервером.\n")
                .version("1.0.0")
                .contact(new Contact().name("Nostya && Dasha")));
        openApi.setServers(List.of(new Server()
                .url("/api/v1")
                .description("Основной сервис управления коллекцией билетов")));
        openApi.setTags(List.of(
                new Tag().name("Tickets").description("Базовые операции над коллекцией билетов"),
                new Tag().name("Ticket queries").description("Специальные операции выборки билетов")
        ));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void applySchemas(OpenAPI openApi) {
        Components components = openApi.getComponents();
        Map<String, Schema> schemas = components.getSchemas();

        Schema ticketType = new StringSchema()
                ._enum(List.of("VIP", "USUAL", "BUDGETARY", "CHEAP"))
                .title("Тип билета")
                .description("Тип билета. Порядок сравнения: VIP < USUAL < BUDGETARY < CHEAP");
        Schema eventType = new StringSchema()
                ._enum(Arrays.asList("CONCERT", "BASKETBALL", "OPERA", null))
                .nullable(true)
                .title("Тип мероприятия")
                .description("Тип мероприятия");
        schemas.put("TicketType", ticketType);
        schemas.put("EventType", eventType);

        replaceProperty(schemas, "Ticket", "type", "TicketType");
        replaceProperty(schemas, "TicketInput", "type", "TicketType");
        replaceProperty(schemas, "TicketPatch", "type", "TicketType");
        replaceProperty(schemas, "Event", "eventType", "EventType");
        replaceProperty(schemas, "EventInput", "eventType", "EventType");

        schema(schemas, "Ticket").title("Билет");
        schema(schemas, "TicketInput").title("Данные билета для создания или полной замены");
        schema(schemas, "TicketPatch")
                .title("Частичное обновление билета")
                .minProperties(1)
                .additionalProperties(false);
        schema(schemas, "Coordinates").title("Координаты");
        schema(schemas, "Event").title("Мероприятие").nullable(true);
        schema(schemas, "EventInput").title("Данные мероприятия для запроса").nullable(true);
        schema(schemas, "ErrorResponse").title("Ошибка API");

        Schema violations = (Schema) schema(schemas, "ErrorResponse")
                .getProperties().get("violations");
        if (violations != null && violations.getItems() != null) {
            violations.getItems().setDescription(null);
        }

        Schema y = (Schema) schema(schemas, "Coordinates").getProperties().get("y");
        y.setMinimum(BigDecimal.valueOf(-495));
        y.setExclusiveMinimum(true);

        Set<Schema> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        schemas.values().forEach(value -> removeGeneratorDefaults(value, visited));
    }

    private void applyParameters(OpenAPI openApi) {
        Operation getTickets = openApi.getPaths().get("/tickets").getGet();
        parameter(getTickets, "page").schema(new IntegerSchema()
                .format("int32").minimum(BigDecimal.ZERO)._default(0));
        parameter(getTickets, "size").schema(new IntegerSchema()
                .format("int32").minimum(BigDecimal.ONE)
                .maximum(BigDecimal.valueOf(100))._default(10));
        arrayParameter(parameter(getTickets, "sort"));
        arrayParameter(parameter(getTickets, "filter"));

        ticketParameter(openApi.getPaths().get("/tickets").getPost(), "TicketInput");
        ticketParameter(openApi.getPaths().get("/tickets/{id}").getPut(), "TicketInput");
        ticketParameter(openApi.getPaths().get("/tickets/{id}").getPatch(), "TicketPatch");

        for (Operation operation : List.of(
                openApi.getPaths().get("/tickets/{id}").getGet(),
                openApi.getPaths().get("/tickets/{id}").getPut(),
                openApi.getPaths().get("/tickets/{id}").getPatch(),
                openApi.getPaths().get("/tickets/{id}").getDelete())) {
            parameter(operation, "id").schema(new IntegerSchema()
                    .format("int64").minimum(BigDecimal.ONE));
        }

        parameter(openApi.getPaths().get("/tickets/{id}").getGet(), "If-None-Match")
                .schema(new StringSchema());
        parameter(openApi.getPaths().get("/tickets/comment-contains").getGet(), "substring")
                .schema(new StringSchema().minLength(1));
        parameter(openApi.getPaths().get("/tickets/type-greater-than").getGet(), "type")
                .schema(new Schema<>().$ref("#/components/schemas/TicketType"));
    }

    private void applyResponseHeaders(OpenAPI openApi) {
        Operation create = openApi.getPaths().get("/tickets").getPost();
        response(create, "201").addHeaderObject("Location", stringHeader("URL созданного билета"));

        Operation getById = openApi.getPaths().get("/tickets/{id}").getGet();
        response(getById, "200").addHeaderObject("ETag", stringHeader(
                "Тег текущего представления билета. Меняется при изменении представления."));
        response(getById, "304").setContent(null);
        response(getById, "304").addHeaderObject("ETag", stringHeader(
                "Версия текущего представления билета"));

        ApiResponse options = response(openApi.getPaths().get("/tickets").getOptions(), "204");
        options.setContent(null);
        options.addHeaderObject("Allow", stringHeader(
                "HTTP-методы, поддерживаемые ресурсом /tickets."));
    }

    private Header stringHeader(String description) {
        return new Header().description(description).schema(new StringSchema());
    }

    private ApiResponse response(Operation operation, String status) {
        return operation.getResponses().get(status);
    }

    private Parameter parameter(Operation operation, String name) {
        return operation.getParameters().stream()
                .filter(value -> name.equals(value.getName()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("OpenAPI parameter not found: " + name));
    }

    private void arrayParameter(Parameter parameter) {
        parameter.setStyle(Parameter.StyleEnum.FORM);
        parameter.setExplode(true);
        parameter.setSchema(new ArraySchema().items(new StringSchema()).minItems(1));
    }

    private void ticketParameter(Operation operation, String schemaName) {
        Parameter parameter = parameter(operation, "ticket");
        parameter.setSchema(null);
        parameter.setContent(new Content().addMediaType(
                "application/json",
                new MediaType().schema(new Schema<>().$ref("#/components/schemas/" + schemaName))
        ));
    }

    @SuppressWarnings("rawtypes")
    private Schema schema(Map<String, Schema> schemas, String name) {
        Schema value = schemas.get(name);
        if (value == null) {
            throw new IllegalStateException("OpenAPI schema not found: " + name);
        }
        return value;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void replaceProperty(Map<String, Schema> schemas, String schemaName,
                                 String propertyName, String referenceName) {
        schema(schemas, schemaName).addProperty(
                propertyName,
                new Schema<>().$ref("#/components/schemas/" + referenceName)
        );
    }

    @SuppressWarnings("rawtypes")
    private void removeGeneratorDefaults(Schema value, Set<Schema> visited) {
        if (value == null || !visited.add(value)) {
            return;
        }
        if (Integer.valueOf(0).equals(value.getMinLength())) {
            value.setMinLength(null);
        }
        if (Integer.valueOf(Integer.MAX_VALUE).equals(value.getMaxLength())) {
            value.setMaxLength(null);
        }
        if (Integer.valueOf(Integer.MAX_VALUE).equals(value.getMaxItems())) {
            value.setMaxItems(null);
        }
        if (value.getProperties() != null) {
            value.getProperties().values().forEach(property ->
                    removeGeneratorDefaults((Schema) property, visited));
        }
        removeGeneratorDefaults(value.getItems(), visited);
    }
}
