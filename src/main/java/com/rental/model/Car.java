package com.rental.model;

import java.util.UUID;

public class Car {

    private final String id;
    private final CarType type;
    private final String licensePlate;

    public Car(CarType type, String licensePlate) {
        this.id = UUID.randomUUID().toString();
        this.type = type;
        this.licensePlate = licensePlate;
    }

    public String getId() { return id; }
    public CarType getType() { return type; }
    public String getLicensePlate() { return licensePlate; }
}
