package com.rental.exception;

import com.rental.model.CarType;
import com.rental.model.ReservationSlot;

public class CarUnavailableException extends RuntimeException {

    public CarUnavailableException(CarType type, ReservationSlot slot) {
        super("No %s available from %s to %s".formatted(type, slot.start(), slot.end()));
    }
}
