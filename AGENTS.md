# AGENTS.md

Project-specific instructions for coding agents working in this repository.

## Primary References

- Treat `ref/bitwig-api-flat/` as the local Bitwig API reference. Check it before guessing about API surface or controller-script behaviour.
- Prefer repository documentation over memory when project-specific behaviour is already documented.
- Relevant architectural decisions live under `doc/adr/`.
- Treat active plans in `doc/dev/` as current design context. Treat `doc/dev/_archive/` as historical context: useful for intent and prior decisions, but not automatically current.

## Codebase Map

- `modules/akai-fire/` is the main active development area for Akai Fire modes.
- `modules/akai-fire/src/main/java/com/oikoaudio/fire/control/` owns shared physical control helpers: encoder behaviour, touch reset, value profiles, and related interaction policies.
- `modules/akai-fire/src/main/java/com/oikoaudio/fire/sequence/` owns drum and step-sequencer behaviour.
- `modules/akai-fire/src/main/java/com/oikoaudio/fire/nestedrhythm/` owns nested rhythm generation and playback logic.
- `modules/akai-fire/src/main/java/com/oikoaudio/fire/melodic/` owns melodic step generation.
- `modules/akai-fire/src/main/java/com/oikoaudio/fire/note/` owns live pitched and harmonic note input.
- `modules/akai-fire/src/main/java/com/oikoaudio/fire/chordstep/` owns chord-step sequencing.
- `modules/akai-fire/src/main/java/com/oikoaudio/fire/perform/` owns clip launcher / perform mode.
- `modules/akai-fire/src/main/java/com/oikoaudio/fire/music/` owns shared pitch and musical context.
- `modules/launchcontrol/` is the Launch Control XL extension; do not apply Akai Fire assumptions there unless explicitly intended.
- `modules/common/` contains copied/shared Bitwig framework helpers. Avoid broad changes there unless required by both controllers.

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
- Before implementing large or interaction-heavy changes, check for a relevant plan in `doc/dev/`.
- If a plan conflicts with current code, trust current code for behaviour and use the plan to understand intent.
- Keep behaviour-preserving refactors separate from feature work unless the user explicitly asks to combine them.
- Use Conventional Commits for commit messages so release-please can classify changes reliably, for example `feat: add melodic arp mode`, `fix: correct encoder reset`, or `docs: update user guide`.

## Controller Architecture Rules

- Preserve controller behaviour unless the task explicitly asks for a UX change.
- Prefer one authoritative owner for physical controls. Avoid duplicate physical truth sources for controls such as `SHIFT`, `ALT`, `SELECT`, `BROWSER`, and encoder touch state.
- Mode classes may own musical semantics, but shared physical state and shared interaction policies should live in control/global helpers.
- Keep global shell behaviour in the extension or explicit shell collaborators; keep mode-specific pad/grid/encoder behaviour in the active mode.
- When extracting helpers, prefer small concrete collaborators over generic routing frameworks.
- Avoid package names, class names, or inheritance structures that preserve obsolete coupling after the responsibility has moved.

## Akai Fire Invariants

- `SHIFT + turn` should remain the fine-adjust path for Akai Fire encoder behaviour unless a task explicitly changes that convention.
- Continuous encoder targets should use the shared accelerated behaviour path where possible.
- Selector/enum-like encoder targets should use thresholded stepping behaviour.
- Touch-hold reset should use shared control-layer helpers rather than mode-local duplicate orchestration.
- Existing top-level mode gestures (`NOTE`, `STEP`, `DRUM`, `PERFORM`) and existing encoder page layouts should not change during structural refactors.
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

- `doc/user-guide.md` is the canonical user guide source.
- Update user-facing documentation at the end of a feature or completed behaviour change, not after every intermediate implementation step.
- If controller behaviour or layout changes by the end of the feature, update `doc/user-guide.md`.
- Bundled in-app help is generated from `doc/user-guide.md` by the Gradle `generateBundledDocumentation` task; do not hand-edit generated `Documentation/index.html` output.
- Record notable code changes in `CHANGES.md` when appropriate.

## Implementation Notes

- Target compatibility should remain aligned with the repo’s documented Bitwig/Java versions unless the task explicitly changes that contract.
- Preserve existing controller conventions and mode vocabulary unless the task intentionally redesigns them.

## Common Regression Areas

Before finishing controller changes, consider impact on:

- mode switching and layer activation
- global modifier state, especially `SHIFT` and `ALT`
- popup browser and global settings overlay
- encoder acceleration, fine adjust, and touch reset
- pad lighting and OLED feedback
- clip cursor / selected clip observation
- Bitwig API lifecycle and init ordering

## When Unsure

- Verify assumptions in code, tests, ADRs, and local reference material before introducing new behaviour.
- If a feature is large or interaction-heavy, write down the gesture or workflow expectations before changing controller mappings.
