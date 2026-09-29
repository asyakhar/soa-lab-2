package ru.ifmo.soa.booking.service;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.Response;

import java.util.concurrent.TimeUnit;

@ApplicationScoped
public class TicketServiceClient {

    private static final String DEFAULT_URL =
            "https://localhost:8443/api/v1";

    private final Client client;
    private final String baseUrl;

    public TicketServiceClient() {
        client = ClientBuilder.newBuilder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .build();

        String configuredUrl = System.getProperty("ticket.service.url");
        if (configuredUrl == null || configuredUrl.isBlank()) {
            configuredUrl = System.getenv("TICKET_SERVICE_URL");
        }
        if (configuredUrl == null || configuredUrl.isBlank()) {
            configuredUrl = DEFAULT_URL;
        }

        baseUrl = removeTrailingSlash(configuredUrl);
    }

    public boolean exists(long ticketId) {
        try (Response response = client.target(baseUrl)
                .path("tickets")
                .path(Long.toString(ticketId))
                .request()
                .get()) {
            if (response.getStatus() == Response.Status.OK.getStatusCode()) {
                return true;
            }
            if (response.getStatus() == Response.Status.NOT_FOUND.getStatusCode()) {
                return false;
            }

            throw new TicketServiceCallException(
                    "Ticket Service вернул неожиданный HTTP-код "
                            + response.getStatus() + "."
            );
        } catch (TicketServiceCallException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new TicketServiceCallException(
                    "Не удалось вызвать Ticket Service.",
                    exception
            );
        }
    }

    @PreDestroy
    public void close() {
        client.close();
    }

    private String removeTrailingSlash(String value) {
        return value.endsWith("/")
                ? value.substring(0, value.length() - 1)
                : value;
    }
}
