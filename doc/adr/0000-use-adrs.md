# ADR 0000 — Use Architecture Decision Records

## Context

The Oikontrol project diverges from Bitwig’s stock Launch Control XL script in several key ways (user-mode routing, arp integration, build setup). We want a lightweight way to capture these decisions so future contributors understand the rationale behind them.

## Decision

Adopt Architecture Decision Records (ADRs) under `doc/adr/` to document significant choices (e.g., starting point, template behaviour, build targets). Each ADR gets a monotonically increasing ID and a short markdown file.

## Status

Accepted.

## Consequences

- Decisions such as “Template 8 hosts the arp” or “Build against Java 21 / API 25” are documented in ADR 0001 and future ADRs.
- Contributors should add a new ADR whenever we make a change that affects architecture, tooling, or controller behaviour.
