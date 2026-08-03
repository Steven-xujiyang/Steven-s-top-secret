# ADR-002: Pure Java Kernel — No Frameworks

**Status:** Accepted
**Date:** 2024-01-02
**Deciders:** quant-platform team

## Context

The backtesting kernel must be deterministic. Framework-managed lifecycles
(Spring DI), JPA entities, and HTTP clients introduce:
- Non-deterministic initialization order;
- Implicit database transactions that affect state visibility;
- Network calls that fail or time out non-deterministically.

## Decision

The kernel is implemented in pure Java 24, using only the standard library
(`java.base`, `java.time`, `java.math`).

### Explicitly allowed
- Java records for immutable value objects
- `BigDecimal` with explicit scale and `RoundingMode.HALF_EVEN` for all
  monetary and price calculations
- `TreeMap` / `TreeSet` for stable iteration order
- `List.copyOf()` / `Collections.unmodifiableMap()` for defensive copies
- JUnit 5, AssertJ, and ArchUnit in `test` scope only

### Explicitly forbidden
- `org.springframework.*`
- `jakarta.persistence.*`, `org.hibernate.*`
- `java.net.http.*`, `org.apache.http.*`
- `java.sql.*`, `javax.sql.*`
- `com.fasterxml.jackson.*`, `com.google.gson.*`
- `java.util.HashMap` when iteration order matters
- `java.util.UUID.randomUUID()`
- `java.time.Instant.now()`
- `java.util.Random` (unseeded)

### Enforcement

Two layers:
1. **Maven Enforcer** `bannedDependencies` rule at compile time.
2. **ArchUnit** `KernelDependencyTest` at test time.

## Consequences

- No dependency injection framework; wiring is explicit in `BacktestEngine`.
- No JSON serialization in the kernel; canonical result uses plain text.
- No database access; all data is in-memory for the kernel's scope.
- The kernel is trivially portable to future Java versions.
