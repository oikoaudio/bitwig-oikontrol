# Controller invariants

Preserve controller behaviour unless the task explicitly asks for a UX change.

## Physical control ownership

- Prefer one authoritative owner for physical controls.
- Avoid duplicate physical truth sources for controls such as `SHIFT`, `ALT`, `SELECT`, `BROWSER`, and encoder touch state.
- Mode classes may own musical semantics, but shared physical state and shared interaction policies should live in control/global helpers.
- Keep global shell behaviour in the extension or explicit shell collaborators.
- Keep mode-specific pad/grid/encoder behaviour in the active mode.
- When extracting helpers, prefer small concrete collaborators over generic routing frameworks.

## Encoder behaviour

- `SHIFT + turn` should remain the fine-adjust path for Akai Fire encoder behaviour unless a task explicitly changes that convention.
- Continuous encoder targets should use the shared accelerated behaviour path where possible.
- Selector/enum-like encoder targets should use thresholded stepping behaviour.
- Touch-hold reset should use shared control-layer helpers rather than mode-local duplicate orchestration.
- Existing encoder page layouts should not change during structural refactors.

## Mode gestures and switching

- Existing top-level mode gestures (`NOTE`, `STEP`, `DRUM`, `PERFORM`) should not change during structural refactors.
- Preserve mode switching and layer activation behaviour.
- Preserve global modifier state, especially `SHIFT` and `ALT`.
- Preserve popup browser and global settings overlay behaviour.

## Rhythm and generator behaviour

- For rhythm/generator work, preserve deterministic musical behaviour unless the spec explicitly introduces randomness.
- Generated rhythms, generated melodic phrases, mutation, recurrence, chance, timing, and velocity changes should be covered by focused tests where practical.

## Feedback and lifecycle

- Preserve pad lighting and OLED feedback.
- Consider clip cursor / selected clip observation before finishing controller changes.
- Respect Bitwig API lifecycle and init ordering.
- Check `ref/bitwig-api-flat/` before guessing about API surface or controller-script behaviour.

## Parallel implementation constraints

- No broad drive-by refactors during parallel implementation.
- Do not rename, move, or reformat unrelated code.
- Treat shared contracts as frozen during parallel issue implementation.
- If a shared contract is wrong, stop and report/block rather than silently changing it.
