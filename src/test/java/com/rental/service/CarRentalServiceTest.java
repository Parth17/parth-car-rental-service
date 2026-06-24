package com.rental.service;

import com.rental.exception.CarUnavailableException;
import com.rental.model.Car;
import com.rental.model.CarType;
import com.rental.model.Reservation;
import com.rental.repository.FleetRepository;
import com.rental.repository.ReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CarRentalServiceTest {

    private static final LocalDateTime JUNE_1 = LocalDateTime.of(2024, 6, 1, 10, 0);

    private FleetRepository fleet;
    private CarRentalService service;

    @BeforeEach
    void setUp() {
        fleet = new FleetRepository();
        service = new CarRentalService(fleet, new ReservationRepository());
    }

    @Test
    void reservesSedanForRequestedPeriod() {
        fleet.add(new Car(CarType.SEDAN, "SD-001"));

        Reservation r = service.reserve(CarType.SEDAN, JUNE_1, 3, "cust-1");

        assertThat(r.getCar().getType()).isEqualTo(CarType.SEDAN);
        assertThat(r.getSlot().start()).isEqualTo(JUNE_1);
        assertThat(r.getSlot().end()).isEqualTo(JUNE_1.plusDays(3));
    }

    @Test
    void reservesSUV() {
        fleet.add(new Car(CarType.SUV, "SV-001"));
        assertThat(service.reserve(CarType.SUV, JUNE_1, 5, "cust-1").getCar().getType()).isEqualTo(CarType.SUV);
    }

    @Test
    void reservesVan() {
        fleet.add(new Car(CarType.VAN, "VN-001"));
        assertThat(service.reserve(CarType.VAN, JUNE_1, 2, "cust-1").getCar().getType()).isEqualTo(CarType.VAN);
    }

    @Test
    void throwsWhenNoCarOfTypeInFleet() {
        assertThatThrownBy(() -> service.reserve(CarType.SEDAN, JUNE_1, 3, "cust-1"))
                .isInstanceOf(CarUnavailableException.class);
    }

    @Test
    void throwsWhenOnlyAvailableCarIsAlreadyBooked() {
        fleet.add(new Car(CarType.SEDAN, "SD-001"));

        service.reserve(CarType.SEDAN, JUNE_1, 5, "cust-1");

        assertThatThrownBy(() -> service.reserve(CarType.SEDAN, JUNE_1.plusDays(2), 3, "cust-2"))
                .isInstanceOf(CarUnavailableException.class);
    }

    @Test
    void fallsBackToSecondCarWhenFirstIsBooked() {
        fleet.add(new Car(CarType.SEDAN, "SD-001"));
        fleet.add(new Car(CarType.SEDAN, "SD-002"));

        service.reserve(CarType.SEDAN, JUNE_1, 5, "cust-1");
        Reservation r = service.reserve(CarType.SEDAN, JUNE_1.plusDays(2), 3, "cust-2");

        assertThat(r.getCar().getLicensePlate()).isEqualTo("SD-002");
    }

    @Test
    void throwsWhenEntireFleetIsBooked() {
        fleet.add(new Car(CarType.VAN, "VN-001"));
        fleet.add(new Car(CarType.VAN, "VN-002"));

        service.reserve(CarType.VAN, JUNE_1, 10, "cust-1");
        service.reserve(CarType.VAN, JUNE_1, 10, "cust-2");

        assertThatThrownBy(() -> service.reserve(CarType.VAN, JUNE_1.plusDays(3), 5, "cust-3"))
                .isInstanceOf(CarUnavailableException.class);
    }

    @Test
    void sameCarCanBeReservedAgainAfterReturnDate() {
        fleet.add(new Car(CarType.SEDAN, "SD-001"));

        service.reserve(CarType.SEDAN, JUNE_1, 3, "cust-1");
        // return is June 4, next pickup is June 4 — should be fine
        Reservation r = service.reserve(CarType.SEDAN, JUNE_1.plusDays(3), 3, "cust-2");

        assertThat(r.getCar().getLicensePlate()).isEqualTo("SD-001");
    }

    @Test
    void rejectsZeroRentalDays() {
        fleet.add(new Car(CarType.SEDAN, "SD-001"));
        assertThatThrownBy(() -> service.reserve(CarType.SEDAN, JUNE_1, 0, "cust-1"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNegativeRentalDays() {
        fleet.add(new Car(CarType.SEDAN, "SD-001"));
        assertThatThrownBy(() -> service.reserve(CarType.SEDAN, JUNE_1, -3, "cust-1"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void storesAllReservations() {
        fleet.add(new Car(CarType.SEDAN, "SD-001"));
        fleet.add(new Car(CarType.SUV, "SV-001"));

        service.reserve(CarType.SEDAN, JUNE_1, 3, "cust-1");
        service.reserve(CarType.SUV, JUNE_1, 5, "cust-2");

        assertThat(service.getReservations()).hasSize(2);
    }
}
