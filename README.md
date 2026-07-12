# Movie Ticket Booking System

A Spring Boot backend for a multi-city, multi-theater movie ticket booking platform:
seat-level booking with time-bound holds, tiered pricing, discount codes, mock
payments, and policy-driven refunds — built for the SDE-2 take-home brief.

> **Note on this submission**: the majority of this codebase was written with Claude
> (Anthropic) as a pair-programmer, in a sandboxed environment with **no internet
> access**, which means `mvn` could never actually run here. The code has been
> written carefully and reviewed line-by-line, but you should run
> `mvn clean verify` yourself before treating it as green. See `Claude.md` for the
> full AI workflow used.

## Table of contents

- [Tech stack](#tech-stack)
- [Architecture](#architecture)
- [Concurrency: no double-booking](#concurrency-no-double-booking)
- [Domain model](#domain-model)
- [API surface](#api-surface)
- [Running it](#running-it)
- [Testing](#testing)
- [Key assumptions & scoping decisions](#key-assumptions--scoping-decisions)
- [What I'd add with more time](#what-id-add-with-more-time)

## Tech stack

| Concern | Choice | Why |
|---|---|---|
| Framework | Spring Boot 3.3 (Java 21) | Mandated by the brief; virtual-thread-ready, modern records/switch expressions used throughout |
| Persistence | Spring Data JPA / Hibernate | Declarative repositories, and gives us `@Version` optimistic locking and `@Lock(PESSIMISTIC_WRITE)` for free — both load-bearing for the concurrency requirement |
| Database | H2 (in-memory, default profile) / PostgreSQL (`postgres` profile) | H2 means anyone can clone and run with zero setup; a real deployment would run on Postgres, which is why the schema avoids H2-only features and the `postgres` Spring profile is already wired up |
| Auth | Spring Security + JWT (HMAC, `jjwt`) | Stateless REST APIs shouldn't carry server-side sessions; JWT is not "advanced auth" (OAuth/SSO/MFA, which are explicitly out of scope) — it's the baseline for a stateless RBAC API |
| Concurrency control | Pessimistic row locks (`SELECT ... FOR UPDATE`) on the seat inventory table | See below — this is the crux of the assignment |
| Async notifications | `@Async` + `@TransactionalEventListener(AFTER_COMMIT)` | Confirmation/reminder "sends" never block the booking request thread, and never fire on a rolled-back transaction |
| Docs | springdoc-openapi | Free Swagger UI at `/swagger-ui.html` from the existing annotations |
| Tests | JUnit 5, AssertJ, Spring Boot Test, MockMvc | Unit tests for pure pricing/refund math, integration tests for the HTTP flows and a dedicated real-multithreaded concurrency test |

## Architecture

Straightforward layered monolith (distributed systems / microservices are explicitly
out of scope):

```
controller/  -> REST endpoints, request validation, HTTP status mapping
service/     -> business rules, transaction boundaries
repository/  -> Spring Data JPA interfaces
entity/      -> JPA entities
dto/         -> request/response shapes (never leak entities over the wire)
security/    -> JWT issuance/validation, UserDetails adapter
scheduler/   -> background sweep for expired holds
event/       -> domain events for decoupled async notifications
exception/   -> typed domain exceptions + a single @RestControllerAdvice
```

## Concurrency: no double-booking

This is the part of the brief I spent the most design effort on, so it's worth
explaining directly.

**Inventory model.** Every physical seat gets exactly one `ShowSeat` row *per show*
(not per booking). `ShowSeat.status` is one of `AVAILABLE`, `HELD`, `BOOKED`. This
row is the single source of truth for "can this seat be taken right now."

**The hold flow** (`SeatHoldService.holdSeats`):
1. Sort the requested `showSeatId`s and take a `SELECT ... FOR UPDATE` lock on
   exactly those rows, inside one short transaction (`ShowSeatRepository.lockForUpdate`).
   Sorting matters: if two requests lock overlapping-but-different seat sets in
   different orders, you get a classic lock-ordering deadlock. Sorting guarantees a
   total order, which rules that out.
2. With the rows locked, check each seat is `AVAILABLE`, or `HELD` with an expired
   `holdExpiresAt` (lazy expiry — see below). If any seat fails that check, the
   whole request fails with `409 Conflict` and *nothing* is written.
3. Otherwise, flip all seats to `HELD`, stamp `holdExpiresAt = now + 5min`
   (configurable), and commit — releasing the row locks immediately.

Because the check-then-write happens under a single row lock held for the shortest
possible window, two concurrent requests for the *same* seat are strictly
serialized by the database: the second transaction blocks until the first commits,
then sees the seat is now `HELD` and fails cleanly. This is proven directly in
`SeatHoldConcurrencyTest`, which fires 20 real threads at the same seat and asserts
exactly one succeeds.

**Booking** (`BookingService.createBooking`) re-locks the same seat rows (same
sort-then-lock discipline) and re-validates the hold belongs to the calling user and
hasn't expired, before flipping `HELD -> BOOKED`. This closes the gap between "you
held it" and "you paid for it" — nobody else can steal or expire-sweep the seat out
from under you mid-checkout, because the booking transaction holds the same locks.

**Hold expiry is two-layered:**
- *Lazy*: `ShowSeat.isHoldExpired(now)` treats an expired `HELD` row as effectively
  `AVAILABLE` the moment anyone reads or tries to lock it — so a stale hold can never
  block a real customer, even if the sweep hasn't run yet.
- *Active*: `SeatHoldExpiryScheduler` runs every 30s (configurable), finds expired
  holds, re-locks them, and flips them back to `AVAILABLE` at rest — so admin views
  and inventory counts stay accurate and there's no unbounded buildup of stale rows.

**Why pessimistic locks and not optimistic-only?** Optimistic locking (`@Version`,
present on every entity) is great when conflicts are rare and you're happy to retry.
Seat selection is the opposite case — for a popular show, many users are expected to
race for the *same* few seats, and I'd rather have the loser fail fast with a clear
"someone else got it" than spin through retries. Optimistic locking still backs
everything else (e.g. two admins editing the same refund policy).

## Domain model

Cities → Theaters → Screens (fixed physical seat layout: `SeatTemplate`, row +
number + `REGULAR`/`PREMIUM`/`RECLINER`) → Shows (a movie scheduled on a screen with
a `PricingTier`) → `ShowSeat` (one row per seat per show; this is the bookable unit,
carrying a price *snapshot* so later pricing-tier edits never retroactively change an
already-scheduled show). `PricingTier` supports arbitrary named tiers (`WEEKDAY`,
`WEEKEND`, `HOLIDAY`, ...) each with its own price per seat type, which is how
"regular / premium / weekend" pricing from the brief is modeled — seat type and
day-type are orthogonal, and a tier is just a price matrix over both.

`Booking` -> `BookingSeat`s (+ a `Payment`) -> optional `DiscountCode`, and a
snapshotted `RefundPolicy` (again, so a later admin edit to the policy doesn't
retroactively change what a customer with an existing booking was promised).

Full entity list: `City`, `Theater`, `Screen`, `SeatTemplate`, `Movie`,
`PricingTier`, `Show`, `ShowSeat`, `DiscountCode`, `RefundPolicy` (+ embedded
`RefundRule` tiers), `Booking`, `BookingSeat`, `Payment`, `User`.

## API surface

All endpoints are under `/api`. Full request/response shapes: run the app and see
`/swagger-ui.html`.

**Auth** (public)
- `POST /api/auth/register` — customer self-registration
- `POST /api/auth/login` — returns a JWT

**Catalog browsing** (public, read-only)
- `GET /api/cities`, `GET /api/theaters?cityId=`, `GET /api/movies`
- `GET /api/shows?cityId=&movieId=&from=&to=` — search
- `GET /api/shows/{id}` , `GET /api/shows/{id}/seats` — seat map with live status

**Booking (customer, JWT required)**
- `POST /api/shows/{id}/holds` — hold one or more seats for 5 minutes
- `DELETE /api/shows/{id}/holds` — release a hold early
- `POST /api/bookings` — convert a hold into a paid, confirmed booking (discount
  code optional)
- `GET /api/bookings` — booking history
- `GET /api/bookings/{id}`
- `POST /api/bookings/{id}/cancel` — cancels + refunds per the applicable policy

**Admin (JWT required, `ADMIN` role)**
- `POST /api/admin/cities`, `DELETE /api/admin/cities/{id}`
- `POST /api/admin/theaters`
- `POST /api/admin/screens` (seat layout defined inline — see below), `GET /api/admin/screens/{id}`
- `POST /api/admin/movies`
- `POST /api/admin/pricing-tiers`, `GET /api/admin/pricing-tiers`
- `POST /api/admin/shows`
- `POST /api/admin/discount-codes`, `GET /api/admin/discount-codes`, `DELETE /api/admin/discount-codes/{id}`
- `POST /api/admin/refund-policies`, `GET /api/admin/refund-policies`

## Running it

Requires JDK 21 and Maven (this sandbox had neither internet access nor `mvn`
installed, so this hasn't been run in-environment — please build locally):

```bash
mvn spring-boot:run
# App on http://localhost:8080, Swagger on http://localhost:8080/swagger-ui.html
# H2 console on http://localhost:8080/h2-console (JDBC URL: jdbc:h2:mem:moviebooking)
```

A default admin account is seeded on first boot (see `AdminBootstrapRunner`) since
public self-registration can only ever create `CUSTOMER` accounts:

```
email:    admin@moviebooking.local
password: Admin@12345
```

Override via `APP_ADMIN_BOOTSTRAP_EMAIL` / `APP_ADMIN_BOOTSTRAP_PASSWORD` env vars,
or set `APP_ADMIN_BOOTSTRAP_ENABLED=false` to disable seeding entirely.

To run against Postgres instead of the in-memory H2 default:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=postgres \
  -DDB_URL=jdbc:postgresql://localhost:5432/moviebooking \
  -DDB_USERNAME=postgres -DDB_PASSWORD=postgres
```

## Testing

```bash
mvn test
```

- `RefundPolicyTest`, `PricingCalculatorTest` — pure unit tests for the refund-tier
  resolution and discount/pricing math, no Spring context.
- `BookingFlowIntegrationTest` — full HTTP walk (MockMvc) through admin setup ->
  customer registration -> browse -> hold -> book (with discount/refund policy in
  play) -> cancel -> refund, plus a same-seat race that asserts the loser gets
  `409 Conflict`.
- `SeatHoldConcurrencyTest` — the one I'd point you to first: 20 real OS threads
  hammering `SeatHoldService.holdSeats` for the *same single seat* simultaneously,
  asserting exactly one wins and the seat ends up `HELD` (not double-allocated, not
  lost).

## Key assumptions & scoping decisions

- **A "hold" has no separate entity/token.** `ShowSeat` itself carries `status`,
  `holdExpiresAt`, and `heldByUser`; the `showSeatId`s returned from the hold call
  double as the hold reference passed into the booking call. Simpler than a parallel
  `SeatHold` table, and there's nothing a separate entity would give us here that the
  seat row itself doesn't already need to track.
- **One default `RefundPolicy` at a time**, snapshotted onto the `Booking` at
  creation time. Admins could reasonably want per-movie or per-theater refund
  policies; I scoped to one global default for time, but the entity is already
  structured so that's a small extension (add an optional FK from `Theater`/`Movie`
  and prefer the more specific one).
- **Pricing tiers are a name + price-per-seat-type map**, assigned per show at
  creation time (not computed dynamically from the show's date/time). An admin
  decides a given show is "WEEKEND" pricing when they schedule it, rather than the
  system inferring it from the calendar. This was a deliberate simplification —
  auto-detecting weekend/holiday pricing is a real feature but adds a rules engine
  that felt out of proportion for the time box.
- **Payments are fully mocked** (`PaymentService`) — there's no real gateway
  integration in scope. It exposes the shape a real integration would (charge/refund
  against a `Booking`, transaction refs), including a `simulatePaymentFailure` flag
  on the booking request so the "payment declined -> seats released" path is
  exercisable deterministically rather than relying on random failures.
- **Discount codes** are simple (percentage or flat, min booking amount, max
  discount cap, validity window, max uses) — no per-user redemption limits or
  stacking rules, since the brief only asked for "discount codes" generically.
- **JWT, not sessions/OAuth.** The brief excludes "advanced authentication (OAuth,
  SSO, MFA)" but a stateless REST API still needs *some* bearer-token mechanism;
  plain HMAC-signed JWTs are the minimal choice that isn't a "session."
- **Admin accounts are seeded, not self-registered** — `POST /api/auth/register`
  always creates a `CUSTOMER`. Promoting a user to `ADMIN` is an out-of-band DB
  operation in this scope (no "manage admins" API), consistent with "advanced auth"
  being out of scope.
- **Notifications are logged, not actually sent.** The brief says confirmations
  should be delivered "without blocking the booking flow" — the async +
  after-commit wiring is real and is what would carry a real
  email/SMS/push integration; swapping `NotificationService`'s three `send*` methods
  for a real provider call is the only change needed.
- **Show-reminder notifications**: the event/listener plumbing exists
  (`ShowReminderEvent`) but nothing currently schedules reminders ahead of a show's
  start time — that would need a second scheduled job querying upcoming confirmed
  bookings, which I scoped out for time. Flagged here rather than silently
  dropped.

## What I'd add with more time

- A reminder-scheduling job (query confirmed bookings for shows starting in N hours,
  publish `ShowReminderEvent` once per booking).
- Per-theater/per-movie refund policy overrides instead of a single global default.
- Idempotency keys on `POST /api/bookings` so a client retry after a network blip
  can't double-charge.
- Rate limiting on the hold endpoint (a determined client could hammer holds to grief
  other customers).
- Pagination on `GET /api/bookings` and the admin list endpoints.
