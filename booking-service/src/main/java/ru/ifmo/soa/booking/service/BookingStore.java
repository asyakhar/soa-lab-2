package ru.ifmo.soa.booking.service;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class BookingStore {

    private final Map<Long, Booking> bookings = new ConcurrentHashMap<>();

    public boolean sell(long ticketId, long personId, double price) {
        Booking booking = new Booking(personId, price);
        return bookings.putIfAbsent(ticketId, booking) == null;
    }

    public int cancelByPerson(long personId) {
        int sizeBefore = bookings.size();
        bookings.entrySet().removeIf(
                entry -> entry.getValue().personId() == personId
        );
        return sizeBefore - bookings.size();
    }

    record Booking(long personId, double price) {
    }
}
