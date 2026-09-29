package ru.ifmo.soa.booking.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BookingStoreTest {

    @Test
    void cancelRemovesOnlySelectedPersonsBookings() {
        BookingStore store = new BookingStore();

        assertTrue(store.sell(1, 10, 100));
        assertTrue(store.sell(2, 20, 200));
        assertFalse(store.sell(1, 30, 300));

        assertEquals(1, store.cancelByPerson(10));
        assertTrue(store.sell(1, 30, 300));
        assertFalse(store.sell(2, 30, 300));
    }
}
