package ru.ifmo.soa.ticket.api;

import ru.ifmo.soa.ticket.model.ErrorResponse;
import ru.ifmo.soa.ticket.model.Ticket;
import ru.ifmo.soa.ticket.model.TicketType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * A delegate to be called by the {@link TicketQueriesApiController}}.
 * Implement this interface with a {@link org.springframework.stereotype.Service} annotated class.
 */
@jakarta.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2026-09-29T15:09:57.766484+03:00[Europe/Moscow]")
public interface TicketQueriesApiDelegate {

    /**
     * @see TicketQueriesApi#getTicketWithMaxType
     */
    ResponseEntity<Ticket> getTicketWithMaxType();

    /**
     * @see TicketQueriesApi#getTicketsByCommentSubstring
     */
    ResponseEntity<List<Ticket>> getTicketsByCommentSubstring( String  substring);

    /**
     * @see TicketQueriesApi#getTicketsWithTypeGreaterThan
     */
    ResponseEntity<List<Ticket>> getTicketsWithTypeGreaterThan( TicketType  type);

}
