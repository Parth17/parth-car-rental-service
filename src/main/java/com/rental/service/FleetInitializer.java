package com.rental.service;

import com.rental.model.Car;
import com.rental.model.CarType;
import com.rental.repository.FleetRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class FleetInitializer implements ApplicationRunner {

    private final FleetRepository fleet;

    public FleetInitializer(FleetRepository fleet) {
        this.fleet = fleet;
    }

    @Override
    public void run(ApplicationArguments args) {
        seed(CarType.SEDAN, 5, "SD");
        seed(CarType.SUV,   3, "SV");
        seed(CarType.VAN,   2, "VN");
    }

    private void seed(CarType type, int count, String prefix) {
        for (int i = 1; i <= count; i++) {
            fleet.add(new Car(type, "%s-%03d".formatted(prefix, i)));
        }
    }
}
