# ADR-001: Modular Monolith Structure

**Status:** Accepted
**Date:** 2024-01-02
**Deciders:** quant-platform team

## Context

The quantitative backtesting platform needs a clear module boundary between the
deterministic kernel (pure computation) and the surrounding infrastructure
(Spring Boot, database, HTTP, UI).

## Decision

The platform is organized as a Maven multi-module project with the following
high-level separation:

```
quant-platform/
├── quant-kernel/              # Pure Java — no framework dependencies
├── quant-architecture-tests/  # ArchUnit tests for the kernel
├── quant-common/              # Shared enums and utilities
├── quant-core/                # Core domain models (existing)
├── quant-data/                # Market data adapters
├── quant-engine/              # Event-driven backtesting engine (existing)
├── quant-strategy/            # Strategy interface and factor library
├── quant-optimizer/           # Parameter optimization
├── quant-risk/                # Risk limits and position sizing
├── quant-analytics/           # Performance analysis
├── quant-server/              # Spring Boot REST API
├── quant-ui/                  # React frontend
└── quant-benchmark/           # Performance benchmarks
```

### Key rules

1. **quant-kernel** has ZERO compile/provided dependencies on Spring, JPA,
   HTTP clients, or JDBC drivers. This is enforced at build time by both
   the Maven Enforcer plugin (`bannedDependencies` rule) and ArchUnit tests.

2. **quant-architecture-tests** contains only ArchUnit tests that verify
   the kernel's dependency rules. It depends on `quant-kernel` for class
   scanning but nothing else.

3. New modules that need framework dependencies (Spring Boot, DB, HTTP)
   go outside the kernel. The kernel remains a pure computation core.

## Consequences

- The kernel is independently testable without starting a Spring context.
- Kernel determinism is guaranteed by the absence of non-deterministic
  library calls (no `Instant.now()`, `UUID.randomUUID()`, `HashMap` iteration).
- The build enforces the boundary: any attempt to add Spring/JPA/HTTP to
  the kernel fails at compile time.
