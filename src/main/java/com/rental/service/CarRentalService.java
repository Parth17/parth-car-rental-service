package com.rental.service;

import com.rental.exception.CarUnavailableException;
import com.rental.model.Car;
import com.rental.model.CarType;
import com.rental.model.Reservation;
import com.rental.model.ReservationSlot;
import com.rental.repository.FleetRepository;
import com.rental.repository.ReservationRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class CarRentalService {

    private final FleetRepository fleetRepository;
    private final ReservationRepository reservationRepository;

    public CarRentalService(FleetRepository fleetRepository, ReservationRepository reservationRepository) {
        this.fleetRepository = fleetRepository;
        this.reservationRepository = reservationRepository;
    }

    public Reservation reserve(CarType type, LocalDateTime from, int days, String customerId) {
        if (days <= 0) throw new IllegalArgumentException("Rental period must be at least 1 day");

        var slot = new ReservationSlot(from, from.plusDays(days));

        Car car = firstAvailable(type, slot)
                .orElseThrow(() -> new CarUnavailableException(type, slot));

        var reservation = new Reservation(car, slot, customerId);
        reservationRepository.save(reservation);
        return reservation;
    }

    public List<Reservation> getReservations() {
        return reservationRepository.findAll();
    }

    private Optional<Car> firstAvailable(CarType type, ReservationSlot slot) {
        return fleetRepository.byType(type).stream()
                .filter(car -> reservationRepository.conflictsFor(car.getId(), slot).isEmpty())
                .findFirst();
    }
}
