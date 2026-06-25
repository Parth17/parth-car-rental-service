# Architecture at a glance


model/
  CarType ← enum: SEDAN | SUV | VAN
  Car ← entity with UUID id + license plate
  ReservationSlot ← record: start/end + overlaps()
  Reservation ← entity linking Car + Slot + customerId

repository/
  FleetRepository ← cars by type (CopyOnWriteArrayList)
  ReservationRepository ← all saved reservations

service/
  CarRentalService ← reserve() — core domain logic
  FleetInitializer ← seeds 5 Sedans, 3 SUVs, 2 Vans in memory

# Notes

## What I spent time on

The availability check is the only non-trivial piece here. I pulled it into `ReservationSlot`
as a record so the overlap logic sits in one place and is easy to test independently.
The condition `start < other.end && end > other.start` handles all cases — partial overlap,
containment, and the adjacent boundary (e.g. return on June 4, pickup on June 4 is allowed).

For allocation I went with first-available. The spec doesn't say anything about fairness
or car rotation so I kept it simple. Swapping in a different strategy would just mean
changing the comparator/sort in `firstAvailable()`.

## What I didn't do

**Cancellation** — would need a status field on Reservation and the conflict check would
need to filter cancelled ones out. Straightforward but skipped for time.

**Thread safety** — there's a race between finding an available car and saving the
reservation. In practice you'd handle this with a DB unique constraint + retry, or
a short-lived lock per car type. With an in-memory list it's not really solvable cleanly
without synchronization I didn't want to add for a car system.

**REST layer** — I left it out. Adding a controller is pure boilerplate and the interesting
logic is all in the service.

**Fleet config** — sizes are hardcoded in `FleetInitializer`. Could move to
`application.yml` but felt like over-engineering for the scope.

## Test strategy

Tests live at the **service layer** — the only layer with meaningful logic. I avoided
mocking the repositories; they are pure in-memory data structures with no I/O side
effects, so using real instances gives higher confidence with no added cost.

Test structure follows: _given fleet state_ → _action_ → _assertion on result or
exception_. Every requirement bullet is covered by at least one test, and boundary
cases (adjacent slots, fleet exhaustion, negative days) are explicit.
