package com.rental.service;

import com.rental.model.ReservationSlot;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReservationSlotTest {

    private static final LocalDateTime BASE = LocalDateTime.of(2024, 6, 1, 10, 0);

    @Test
    void overlappingSlots() {
        var a = new ReservationSlot(BASE, BASE.plusDays(5));
        var b = new ReservationSlot(BASE.plusDays(3), BASE.plusDays(8));

        assertThat(a.overlaps(b)).isTrue();
        assertThat(b.overlaps(a)).isTrue(); // symmetric
    }

    @Test
    void adjacentSlotsDoNotOverlap() {
        var a = new ReservationSlot(BASE, BASE.plusDays(3));
        var b = new ReservationSlot(BASE.plusDays(3), BASE.plusDays(6));

        assertThat(a.overlaps(b)).isFalse();
    }

    @Test
    void disjointSlots() {
        var a = new ReservationSlot(BASE, BASE.plusDays(2));
        var b = new ReservationSlot(BASE.plusDays(5), BASE.plusDays(7));

        assertThat(a.overlaps(b)).isFalse();
    }

    @Test
    void containedSlotOverlaps() {
        var outer = new ReservationSlot(BASE, BASE.plusDays(10));
        var inner = new ReservationSlot(BASE.plusDays(2), BASE.plusDays(4));

        assertThat(outer.overlaps(inner)).isTrue();
    }

    @Test
    void rejectsInvertedRange() {
        assertThatThrownBy(() -> new ReservationSlot(BASE.plusDays(3), BASE))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
