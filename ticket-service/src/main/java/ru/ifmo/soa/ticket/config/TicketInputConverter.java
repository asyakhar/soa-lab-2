package ru.ifmo.soa.ticket.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;
import ru.ifmo.soa.ticket.model.TicketInput;

@Component
public class TicketInputConverter
        implements Converter<String, TicketInput> {

    private final ObjectReader reader;

    public TicketInputConverter(ObjectMapper objectMapper) {
        this.reader = objectMapper
                .readerFor(TicketInput.class)
                .with(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    @Override
    public TicketInput convert(String source) {
        try {
            return reader.readValue(source);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException(
                    "Параметр ticket содержит неразбираемый JSON.",
                    exception
            );
        }
    }
}