# Design Decisions & Known Tradeoffs

## What I focused on

### Domain model clarity
The core insight is that availability is a **slot overlap problem**. I extracted this into
`ReservationSlot`, a Java record with a single `overlaps()` method — the most testable
and reusable unit in the system. All business rules depend on this method; keeping it
isolated makes it trivially verifiable.

### `findFirstAvailable` — intentionally simple
The allocation strategy picks the **first available car** of the requested type. This is
a conscious choice: the domain does not specify a preference ordering (e.g. least-used
car, cars expiring soonest). First-available is deterministic, easy to reason about in
tests, and easy to swap out with a `Comparator` if ordering requirements emerge.

### `CopyOnWriteArrayList` for thread-safety
In-memory stores use `CopyOnWriteArrayList` so reads are lock-free and concurrent
reservations don't corrupt state. For a real system this would be replaced by a
transactional DB with a `SELECT FOR UPDATE` or optimistic locking.

### No persistence layer abstraction over Spring Data
I used a plain repository class instead of `JpaRepository`. The exercise specifies no DB,
and adding JPA would require entities, a datasource config, and schema DDL — all
infrastructure boilerplate the brief explicitly said to avoid.

## Known limitations

| Area | Limitation | What a production system would do |
|---|---|---|
| Concurrency | Two threads could both pass `findFirstAvailable` before either saves | Use DB transaction + unique constraint, or a `synchronized` block / lock per car |
| Cancellation | Not implemented | Add `cancel(reservationId)` that marks the reservation inactive |
| Pricing | Not modelled | Rate card per `CarType` + duration multiplier |
| Customer identity | String `customerId`, no validation | Validated customer entity with auth |
| Fleet size | Hardcoded in `FleetInitializer` | Configurable via `application.yml` or DB seed |
| REST API | None | `ReservationController` with `POST /reservations` returning `201 Created` |
| Error responses | Exception only | `@ControllerAdvice` mapping exceptions to RFC 7807 problem detail JSON |

