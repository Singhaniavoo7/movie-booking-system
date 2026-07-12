# Claude.md — AI workflow used for this submission

This file documents how Claude (Anthropic) was used to build this project, per the
"AI workflow" requirement in the take-home instructions.

## Tool & setup

- **Model**: Claude (Sonnet-class), used via Claude's agentic coding tools (bash,
  file read/write) in a sandboxed Linux container.
- **Critical constraint**: the sandbox had **no internet access** and no `mvn`
  binary installed. This meant Claude could not run `mvn compile`, `mvn test`, or
  download any dependency during development — every file was written by hand and
  reviewed by re-reading, not by iterating against a compiler or test runner. This
  is called out explicitly so the reviewer knows to run `mvn clean verify` before
  trusting the build, and it's the main reason the natural "write code -> run tests
  -> fix -> repeat" loop wasn't available here.

## Approach

1. **Scope the interpretation first.** The brief is intentionally open-ended, so
   the first step (done conversationally, not as code) was deciding the entity
   model, which flows count as "core," and where to draw the line on features like
   pricing tiers, refund policies, and holds — documented in the README's
   "Key assumptions" section rather than guessed silently.
2. **Design the concurrency model before writing any booking code.** Because "no
   double-allocation under concurrent booking" is the one hard-correctness
   requirement in the brief, the seat-locking strategy (sort-then-`SELECT FOR
   UPDATE`, short critical sections, lazy + active hold expiry) was decided and
   written up before any controller or DTO code, so the rest of the design could be
   built around it rather than retrofitted.
3. **Build bottom-up**: `pom.xml` -> entities -> repositories -> exceptions ->
   security -> services -> controllers -> tests -> docs, in that order, so that
   every layer only ever depended on already-written code below it.
4. **Extract pure logic for testability.** Discount/refund math was deliberately
   pulled out of `BookingService` into a dependency-free `PricingCalculator`
   component specifically so it could be unit-tested without a Spring context —
   useful in general, and especially valuable here given Claude couldn't run the
   tests itself to confirm they'd pass.
5. **Write the concurrency test as the centerpiece**, not an afterthought:
   `SeatHoldConcurrencyTest` fires 20 real threads at the same seat because a
   sequential/mocked test wouldn't actually exercise the row-locking behavior the
   brief is asking to be proven.
6. **Self-review pass**: after each file, re-read it for import correctness,
   Lombok annotation consistency (`@Builder.Default` on every mutable-default
   field), and cross-file consistency (e.g. DTO field names matching what
   controllers/tests send). Several issues were caught and fixed this way,
   including tightening the security config's public routes from whole-path
   `permitAll()` to GET-only `permitAll()` (mutations under `/api/cities` etc.
   still require `ADMIN`), and extracting `PricingCalculator` out of
   `BookingService` mid-build for testability.

## What a human should still do

- Run `mvn clean verify` — nothing here has been compiler- or test-runner-verified.
- Skim the concurrency section of the README and `SeatHoldService` /
  `BookingService` particularly closely, since that's the part of the system where
  a subtle bug would be most costly.
- Decide on the still-open scoping questions called out in the README (per-venue
  refund policies, reminder scheduling, idempotency keys) if they matter for your
  use case.

## Skills / conventions referenced

No Anthropic "Skill" packages applied here beyond standard Java/Spring Boot
conventions (Lombok for boilerplate, constructor injection via
`@RequiredArgsConstructor`, DTOs never leaking entities over the wire, one
`@RestControllerAdvice` for error mapping). Package layout and naming follow
standard Spring Boot convention (`controller` / `service` / `repository` /
`entity` / `dto` / `security` / `exception`).
