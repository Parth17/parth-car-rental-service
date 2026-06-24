package com.rental.repository;

import com.rental.model.Car;
import com.rental.model.CarType;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Repository
public class FleetRepository {

    private final Map<CarType, List<Car>> fleet = new EnumMap<>(CarType.class);

    public void add(Car car) {
        fleet.computeIfAbsent(car.getType(), t -> new ArrayList<>()).add(car);
    }

    public List<Car> byType(CarType type) {
        return List.copyOf(fleet.getOrDefault(type, List.of()));
    }
}
