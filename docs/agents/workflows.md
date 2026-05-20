# Agent workflows

The workflow is human-driven. Agents execute only the stage explicitly requested in the current task.

```text
grill-with-docs
  -> to-prd
  -> to-issues
  -> GitHub issue publishing
  -> implementation
  -> parallel implementation
  -> integration pass
```

Do not advance from one stage to the next without an explicit user request.

## grill-with-docs

Stress-test a plan against current source, tests, ADRs, `GLOSSARY.md`, and reference material.

Outputs may include clarified terminology, open questions, and durable documentation updates to `GLOSSARY.md` or `docs/adr/` when decisions crystallise.

Stop after the grilling session. Do not create a PRD unless the user asks for `to-prd`.

## to-prd

Convert the agreed feature intent into a PRD draft or approved GitHub PRD parent issue, depending on the user's explicit request.

A PRD parent issue is authoritative for intended feature behaviour while active. It is not proof of current code behaviour.

Stop after the PRD stage. Do not generate implementation issues unless the user asks for `to-issues`.

## to-issues

Break the PRD into linked vertical-slice implementation issues.

Each implementation issue should include:

- goal
- user-visible behaviour
- non-goals
- dependencies on other slices/issues
- whether it can run in parallel
- acceptance criteria
- likely files/modules touched
- files/modules to avoid
- test strategy
- integration risks
- expected labels/state

Stop after issue drafting unless the user explicitly asks to publish GitHub Issues.

## GitHub issue publishing

Publish approved PRDs as GitHub parent issues and approved slices as linked GitHub implementation issues.

Publish issues in dependency order so blockers have real issue identifiers.

Do not publish, label, or modify issues unless the user explicitly asks.

## Implementation

Implement one approved issue at a time.

Use TDD where practical. Keep the issue's acceptance criteria visible, verify likely files/modules before editing, and avoid files/modules the issue says to avoid.

## Parallel implementation

Run multiple agents only when the user explicitly asks for parallel implementation and the selected issues satisfy `docs/agents/parallel-work.md`.

## Integration pass

After related slices land, run an integration pass to verify behaviour across shared controller state, mode switching, pad lighting/OLED feedback, Bitwig lifecycle ordering, and docs.

The integration pass should reconcile issue outcomes with durable docs, not turn scratch notes into permanent truth.
