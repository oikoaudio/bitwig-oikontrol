# AGENTS.md

Project-specific instructions for coding agents working in this repository.

## Primary References

- Treat `ref/bitwig-api-flat/` as the local Bitwig API reference. Check it before guessing about API surface or controller-script behaviour.
- Prefer repository documentation over memory when project-specific behaviour is already documented.
- Relevant architectural decisions live under `doc/adr/`.

## Delivery Process

- Default to test-driven development for code changes: add or update a failing test first when the change is testable, then implement, then rerun the relevant tests.
- For larger features, define acceptance criteria before implementation.
- Start from `doc/_feature_template.md` when drafting a new implementation plan.
- Store feature plans in a clearly named markdown doc under `doc/dev/feature/` when that path is being used locally, or another appropriate tracked location under `doc/` when needed.
- Favor code that is clear and readable over code that is merely clever or maximally reusable.
- Small and medium refactors are welcome when they make the resulting code materially cleaner, easier to understand, or easier to change safely.
- Be cautious about DRY-driven abstractions that collapse distinct behaviors into powerful but hard-to-grasp helpers. Prefer explicit code when it keeps intent obvious.
- When package layout, class names, or inheritance still reflect obsolete coupling, treat that as a valid refactor opportunity if cleaning it up will make the design easier to understand.
- Before committing to an implementation approach, reflect on:
  - whether the intent is understood
  - if there is existing solution: if that solution is acceptable
  - whether the change can be done better in place
  - whether a refactor would make the result substantially better

## Documentation Maintenance

- `doc/user-guide.md` is the canonical user guide source.
- If controller behaviour or layout changes, update `doc/user-guide.md`.
- If Akai Fire layout details change, also update `doc/akai-fire-controller-layout.md` unless that content has been merged into the user guide.
- The bundled in-app help pages at `modules/akai-fire/src/main/resources/Documentation/index.html` and `modules/launchcontrol/src/main/resources/Documentation/index.html` should stay in sync with `doc/user-guide.md` when user-facing documentation changes.
- Record notable code changes in `CHANGES.md` when appropriate.

## Implementation Notes

- Prefer `just` targets from `Justfile` for common build and test workflows when they cover the task.
- Target compatibility should remain aligned with the repo’s documented Bitwig/Java versions unless the task explicitly changes that contract.
- Preserve existing controller conventions and mode vocabulary unless the task intentionally redesigns them.

## When Unsure

- Verify assumptions in code, tests, ADRs, and local reference material before introducing new behaviour.
- If a feature is large or interaction-heavy, write down the gesture or workflow expectations before changing controller mappings.
