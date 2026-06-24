# OOP Concepts in This Project

A walkthrough of where and how each object-oriented principle shows up in the codebase, with the actual code that demonstrates it.

---

## 1. Encapsulation

> *Bind data and the rules that operate on it together, and hide the internals from the outside.*

### `ReservationSlot` — owns its own validation and overlap rule

```java
public record ReservationSlot(LocalDateTime start, LocalDateTime end) {

    public ReservationSlot {
        if (start == null || end == null) throw new IllegalArgumentException("start and end are required");
        if (!start.isBefore(end)) throw new IllegalArgumentException("start must be before end");
    }

    public boolean overlaps(ReservationSlot other) {
        // two slots overlap if neither ends before the other starts
        return start.isBefore(other.end) && end.isAfter(other.start);
    }
}
```

No external class can create an invalid slot (end before start, nulls). The overlap rule lives in exactly one place. Callers just ask `slot.overlaps(other)` — they never touch the internals.

### `Car` — generates and guards its own identity

```java
public class Car {

    private final String id;
    private final CarType type;
    private final String licensePlate;

    public Car(CarType type, String licensePlate) {
        this.id = UUID.randomUUID().toString();
        // ...
    }
}
```

The `id` field is `private final`. Callers pass a type and a plate; the car decides its own identity. Nothing outside can set or change it.

### `FleetRepository` — hides how cars are stored

```java
private final Map<CarType, List<Car>> fleet = new EnumMap<>(CarType.class);

public void add(Car car) {
    fleet.computeIfAbsent(car.getType(), t -> new ArrayList<>()).add(car);
}

public List<Car> byType(CarType type) {
    return List.copyOf(fleet.getOrDefault(type, List.of()));
}
```

The internal `EnumMap` is private. `byType()` returns an unmodifiable copy — callers can read the fleet but can't mutate the repository's internal state.

---

## 2. Abstraction

> *Expose what something does, not how it does it.*

### `CarRentalService.reserve()` — hides the allocation strategy

```java
public Reservation reserve(CarType type, LocalDateTime from, int days, String customerId) {
    var slot = new ReservationSlot(from, from.plusDays(days));

    Car car = firstAvailable(type, slot)
            .orElseThrow(() -> new CarUnavailableException(type, slot));

    var reservation = new Reservation(car, slot, customerId);
    reservationRepository.save(reservation);
    return reservation;
}

private Optional<Car> firstAvailable(CarType type, ReservationSlot slot) {
    return fleetRepository.byType(type).stream()
            .filter(car -> reservationRepository.conflictsFor(car.getId(), slot).isEmpty())
            .findFirst();
}
```

`reserve()` doesn't know how a free car is found — it just calls `firstAvailable()`. The allocation strategy (currently first-available) is hidden behind a private method. Changing it to round-robin or least-used means touching one method and nothing else.

### `ReservationRepository.conflictsFor()` — hides query logic

```java
public List<Reservation> conflictsFor(String carId, ReservationSlot slot) {
    return store.stream()
            .filter(r -> r.getCar().getId().equals(carId) && r.getSlot().overlaps(slot))
            .toList();
}
```

The service doesn't iterate over reservations or check overlap directly. It asks the repository "do any conflicts exist for this car in this slot?" and acts on the answer.

---

## 3. Single Responsibility Principle

> *A class should have one reason to change.*

Each class in the project has a clearly scoped job:

| Class | Responsibility |
|---|---|
| `ReservationSlot` | Represent a time window and determine overlap |
| `Car` | Hold vehicle identity and type |
| `Reservation` | Bind a car, a slot, and a customer into a booking |
| `FleetRepository` | Store and retrieve cars by type |
| `ReservationRepository` | Store reservations and find conflicts |
| `CarRentalService` | Orchestrate the reservation flow |
| `FleetInitializer` | Seed the fleet on startup |
| `CarUnavailableException` | Represent the domain error when no car is free |

`FleetInitializer` is a deliberate example of this. Its only job is seeding data on startup. It could have been part of the service, but that would give the service two reasons to change: business logic changes *and* fleet initialisation changes.

---

## 4. Immutability as a design choice

> *Objects that can't change after construction are easier to reason about and test.*

### `ReservationSlot` — Java record (structurally immutable)

Records in Java are implicitly `final` with private fields. There is no way to mutate a slot after it's created. This matters because a saved `Reservation` must not have its time window silently changed by some other part of the system.

### `Car` and `Reservation` — all fields are `final`

```java
private final String id;
private final CarType type;
private final String licensePlate;
```

Once constructed, neither a car nor a reservation can change. This makes them safe to pass around without defensive copying.

### Repositories return unmodifiable views

```java
public List<Car> byType(CarType type) {
    return List.copyOf(fleet.getOrDefault(type, List.of()));
}

public List<Reservation> findAll() {
    return List.copyOf(store);
}
```

Callers get a snapshot of the data. Modifying the returned list doesn't affect the repository's internal state.

---

## 5. Domain modelling — value objects vs entities

A distinction OOP gets right when applied well:

**Value objects** are defined by their content. Two `ReservationSlot` instances with the same start and end are the same thing — identity doesn't matter. Java records express this naturally: structural equality is built in.

**Entities** are defined by their identity. Two `Car` instances with the same type and plate are still different cars. `Car` and `Reservation` are entities — they carry a UUID that survives through the system.

This is why `ReservationSlot` is a `record` and `Car` is a `class`.

---

## 6. Enum for a closed set of values

```java
public enum CarType {
    SEDAN, SUV, VAN
}
```

Car types are a fixed domain concept. An enum rules out invalid values at compile time — no string matching, no null checks, no typos. The compiler catches `CarType.MINIVAN` immediately.

`FleetRepository` uses `EnumMap` as a consequence:

```java
private final Map<CarType, List<Car>> fleet = new EnumMap<>(CarType.class);
```

`EnumMap` is the correct collection when keys are an enum — it's backed by an array indexed by ordinal, so it's faster and more memory-efficient than `HashMap` for this use case.

---

## 7. Exception as a domain concept

```java
public class CarUnavailableException extends RuntimeException {

    public CarUnavailableException(CarType type, ReservationSlot slot) {
        super("No %s available from %s to %s".formatted(type, slot.start(), slot.end()));
    }
}
```

`CarUnavailableException` extends `RuntimeException` (unchecked) because unavailability is a business outcome, not a recoverable error the caller is expected to handle with try-catch boilerplate. The message is generated from domain objects, not hardcoded strings. The exception itself is a domain concept — it speaks the language of the system.

---

## 8. Dependency injection over hard-coded dependencies

`CarRentalService` receives its dependencies through the constructor:

```java
public CarRentalService(FleetRepository fleetRepository, ReservationRepository reservationRepository) {
    this.fleetRepository = fleetRepository;
    this.reservationRepository = reservationRepository;
}
```

This is why the tests can wire a fresh repository per test without any mocking framework:

```java
@BeforeEach
void setUp() {
    fleet = new FleetRepository();
    service = new CarRentalService(fleet, new ReservationRepository());
}
```

Spring manages this in production via `@Service` and `@Repository`. In tests, plain constructor injection is enough. The service doesn't care where its dependencies come from.
