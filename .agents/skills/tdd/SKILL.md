# tdd

## Purpose

Implement one approved issue in one JJ workspace/change. Prefer red-green-refactor where practical.

Test behavior at the narrowest useful public boundary where feasible. Avoid brittle tests against implementation details, use one failing test/implementation/refactor cycle at a time, keep the diff narrow, and do not broaden scope.

Do not parallelize foundation work unless explicitly approved.

## When To Use

Use this skill for implementation of a single approved issue that has clear acceptance criteria and is ready for agent work.

Do not use it to invent scope, resolve unclear product intent, or start unrelated cleanup.

## Testing Philosophy

Tests should describe behavior the caller, user, or neighboring system can observe. Prefer the smallest public boundary that proves the behavior without coupling the test to private structure.

Public boundaries depend on the project. They may be exported library APIs, CLI commands, HTTP endpoints, use-case services, UI flows, plugin callbacks, domain ports, or other stable interfaces.

Prefer real code paths through those boundaries. Use narrower domain tests, contract tests, property tests, golden/approval tests, or adapter tests when those better fit the domain than a broad integration test.

Avoid tests that break when internals are renamed, moved, or refactored while behavior stays the same. Private methods, internal call counts, internal collaboration order, and transient data shapes are usually poor test targets.

## Mocking Guidance

Mock or fake system boundaries when real dependencies are slow, nondeterministic, expensive, unsafe, or unavailable. Common boundaries include external APIs, time, randomness, filesystem access, devices, network services, and sometimes databases.

Prefer not to mock modules you own just to inspect internal collaboration. When an owned subsystem is too heavy to use directly, introduce a clear boundary or test double that represents a real project interface.

Design for testability by passing dependencies in, returning observable results where practical, and keeping public interfaces small enough to exercise clearly.

## Interface Design

Prefer deep modules: small, stable public interfaces that hide meaningful implementation complexity.

Avoid shallow abstractions that add names, files, or indirection without reducing what callers need to know.

During refactor, consider whether new behavior can make an existing module deeper, but do not introduce broad architecture changes outside the issue scope.

## Anti-Patterns

Do not write all tests first and then all implementation. That horizontal approach often locks in imagined behavior and brittle structure before the design has learned anything.

Prefer vertical tracer bullets: write one behavior test, make it pass, refactor while green, then repeat for the next behavior.

## Inputs To Inspect

- The approved issue
- Parent PRD and acceptance criteria
- Dependencies and blocked/ready state
- Relevant code, tests, and public interfaces
- `GLOSSARY.md`
- Relevant ADRs and docs
- Project test commands from local docs or repository conventions

## Process

1. Confirm the issue is approved and unblocked.
2. Identify the narrowest behavior to test first.
3. Choose the smallest useful public boundary for that behavior.
4. Add or update one failing test that expresses observable behavior.
5. Run the focused check and confirm the expected failure when practical.
6. Implement the smallest change that passes the test.
7. Run the focused check again.
8. Refactor only within the issue scope while keeping tests green.
9. Repeat for the next acceptance criterion.
10. Run broader checks appropriate to the risk.
11. Prepare integration notes for follow-up work.

## Per-Cycle Checklist

- Test describes observable behavior, not implementation structure.
- Test uses a stable public boundary or a deliberate project interface.
- Test would survive an internal refactor that preserves behavior.
- Mocks/fakes represent real system boundaries or explicit ports.
- Code is minimal for the current behavior.
- Refactor happens only while checks are green.
- No speculative features or adjacent issue scope are added.

## Output Format

Report:

- Issue implemented
- Changed files
- Behavior covered
- Checks run
- Checks not run
- Risks or caveats
- Integration notes

## Stop Conditions

Stop when the approved issue is implemented and verified as far as practical, or when a blocker prevents responsible progress.

Do not broaden scope, implement adjacent issues, create new foundation work, publish issues, or start parallel agents unless explicitly instructed.
