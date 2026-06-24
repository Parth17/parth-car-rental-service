package com.rental.repository;

import com.rental.model.Reservation;
import com.rental.model.ReservationSlot;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class ReservationRepository {

    private final List<Reservation> store = new ArrayList<>();

    public void save(Reservation reservation) {
        store.add(reservation);
    }

    public List<Reservation> conflictsFor(String carId, ReservationSlot slot) {
        return store.stream()
                .filter(r -> r.getCar().getId().equals(carId) && r.getSlot().overlaps(slot))
                .toList();
    }

    public List<Reservation> findAll() {
        return List.copyOf(store);
    }
}
