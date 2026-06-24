package com.rental.model;

import java.util.UUID;

public class Reservation {

    private final String id;
    private final Car car;
    private final ReservationSlot slot;
    private final String customerId;

    public Reservation(Car car, ReservationSlot slot, String customerId) {
        this.id = UUID.randomUUID().toString();
        this.car = car;
        this.slot = slot;
        this.customerId = customerId;
    }

    public String getId() { return id; }
    public Car getCar() { return car; }
    public ReservationSlot getSlot() { return slot; }
    public String getCustomerId() { return customerId; }
}
