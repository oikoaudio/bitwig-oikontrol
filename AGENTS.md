# AGENTS.md

Project-specific instructions for coding agents working in this repository.

## Primary References

- Treat `ref/bitwig-api-flat/` as the local Bitwig API reference. Check it before guessing about API surface or controller-script behaviour.
- Prefer repository documentation over memory when project-specific behaviour is already documented.
- Relevant architectural decisions live under `docs/adr/`.
- Treat active plans in `docs/dev/` as current design context. Treat `docs/dev/archive/` as historical context: useful for intent and prior decisions, but not automatically current.
- Agent workflow guidance lives under `docs/agents/`; start with `docs/agents/workflows.md` and `docs/agents/parallel-work.md` for PRD/issues/parallel implementation work.
- Domain vocabulary lives in `GLOSSARY.md`; see `docs/agents/domain.md` for how to use it.
- GitHub issue and label conventions live in `docs/agents/issue-tracker.md` and `docs/agents/triage-labels.md`.

## Codebase Map

- Detailed codebase map: `docs/agents/codebase-map.md`.
- `modules/akai-fire/` is the main active development area for Akai Fire modes.
- `modules/launchcontrol/` is the Launch Control XL extension; do not apply Akai Fire assumptions there unless explicitly intended.
- `modules/common/` contains copied/shared Bitwig framework helpers. Avoid broad changes there unless required by both controllers.

## Delivery Process

- For PRD, issue-splitting, implementation, parallel work, and integration stages, follow `docs/agents/workflows.md`; do not advance workflow stages without an explicit user request.
- Default to test-driven development for code changes: add or update a failing test first when the change is testable, then implement, then rerun the relevant tests.
- For larger features, define acceptance criteria before implementation.
- Favor code that is clear and readable over code that is merely clever or maximally reusable.
- Be cautious about DRY-driven abstractions that collapse distinct behaviors into powerful but hard-to-grasp helpers. Prefer explicit code when it keeps intent obvious.
- Before implementing large or interaction-heavy changes, check for a relevant local plan in `docs/dev/` and any tracked/linked feature plan or PRD.
- If a plan conflicts with current code, trust current code for behaviour and use the plan to understand intent.
- Keep behaviour-preserving refactors separate from feature work unless the user explicitly asks to combine them.
- Use Conventional Commits for commit messages so release-please can classify changes reliably, for example `feat: add melodic arp mode`, `fix: correct encoder reset`, or `docs: update user guide`.

## JJ And GitHub PR Workflow

- Human JJ command examples and Hunk setup live in `docs/contributing/jj-cheat-sheet.md`.
- Do not create, approve, or merge GitHub pull requests unless the user explicitly asks for that operation in the current conversation.
- This repository uses JJ locally, so expect Git to be in a detached-head state. Prefer JJ for local state changes and `gh --repo oikoaudio/bitwig-oikontrol ...` for GitHub operations that should not infer a current Git branch.
- For parallel implementation, use one JJ workspace/change per approved issue and follow `docs/agents/parallel-work.md`.
- Before creating a PR, check `jj status`, inspect the commit range since the previous bookmark or merge-domain boundary, and run the relevant tests.
- When the working copy is an empty JJ change on top of the actual feature tip, create the PR bookmark on the parent commit with `jj bookmark create <name> -r @-`; otherwise create it on the intended non-empty revision.
- To merge a user-approved PR, first check it with `gh pr view --repo oikoaudio/bitwig-oikontrol <number> --json state,mergeStateStatus,reviewDecision,headRefName,baseRefName,url`. If merging, use `gh pr merge --repo oikoaudio/bitwig-oikontrol <number> --merge --delete-branch` so `gh` does not depend on the local detached Git HEAD.
- After a merge, refresh local JJ state with `jj git fetch --remote origin`, move local `main` to the fetched remote with `jj bookmark move main --to main@origin`, and start a fresh empty working change with `jj new main`.
- Verify the refreshed state with `jj status`, `jj log -r 'ancestors(@, 5)'`, and `jj bookmark list`.

## Controller Invariants

- Detailed controller invariants: `docs/agents/controller-invariants.md`.
- Preserve controller behaviour unless the task explicitly asks for a UX change.
- Keep shared physical state and interaction policies in control/global helpers, not duplicated in mode-local code.
- For rhythm/generator work, preserve deterministic musical behaviour unless the spec explicitly introduces randomness.

## Build And Test Commands

- Prefer `just` targets from `Justfile` for common build and test workflows when they cover the task.
- Akai Fire compile check: `just fire-compile`
- Akai Fire tests: `just fire-test`
- Akai Fire extension build: `just fire-extension`
- Launch Control XL tests: `just launchcontrol-test`
- Full test suite: `just test`
- Full build: `just build`
- For narrow Java test runs, use Gradle directly with `GRADLE_USER_HOME=/tmp/gradle-home ./gradlew --no-daemon :modules:akai-fire:test --tests <TestClass>`.

## Documentation Maintenance

- `docs/user-guide.md` is the canonical user guide source.
- Update user-facing documentation at the end of a feature or completed behaviour change, not after every intermediate implementation step.
- If controller behaviour or layout changes by the end of the feature, update `docs/user-guide.md`.
- Bundled in-app help is generated from `docs/user-guide.md` by the Gradle `generateBundledDocumentation` task; do not hand-edit generated `Documentation/index.html` output.
- Record notable code changes in `CHANGES.md` when appropriate.

## Implementation Notes

- Target compatibility should remain aligned with the repo’s documented Bitwig/Java versions unless the task explicitly changes that contract.
- Preserve existing controller conventions and mode vocabulary unless the task intentionally redesigns them.

## When Unsure

- Verify assumptions in code, tests, ADRs, and local reference material before introducing new behaviour.
- If a feature is large or interaction-heavy, write down the gesture or workflow expectations before changing controller mappings.
