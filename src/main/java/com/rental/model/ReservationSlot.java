package com.rental.model;

import java.time.LocalDateTime;

public record ReservationSlot(LocalDateTime start, LocalDateTime end) {

    public ReservationSlot {
        if (start == null || end == null) throw new IllegalArgumentException("start and end are required");
        if (!start.isBefore(end)) throw new IllegalArgumentException("start must be before end");
    }

    public boolean overlaps(ReservationSlot other) {
        return start.isBefore(other.end) && end.isAfter(other.start);
    }
}
