package ru.ifmo.soa.booking.service;

import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class TicketServiceCallExceptionMapper
        implements ExceptionMapper<TicketServiceCallException> {

    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(TicketServiceCallException exception) {
        return Response.status(Response.Status.BAD_GATEWAY)
                .type(MediaType.APPLICATION_JSON)
                .entity(ErrorResponses.create(
                        502,
                        "Bad Gateway",
                        exception.getMessage(),
                        uriInfo.getPath()
                ))
                .build();
    }
}
