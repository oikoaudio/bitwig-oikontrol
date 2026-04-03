## Akai Fire (Oiko) – delta vs. upstream rhbitwig

- Packaging: separate Gradle module (`modules/akai-fire`), new UUID `c1f8d20a-3da4-4d2c-8ce1-8b2aa6a8e5b6`, name “Akai Fire by Oiko Audio”.
- Nudge: Grid+arrow buttons left/right for coarse 16th shift of the selected `pad`. Shift+Grid = fine nudge of the selected pad. Held-step micro-nudge disabled for stability for now. 
- Euclid mode: User2 encoders = LEN/PULS/ROT/INV, Browser = apply, Shift=preview placeholder; patterns tile across clip length.
- Play button: Alt+Play retriggers current clip; regular Play toggles transport and retriggers on start.

Notes on behavior in rhbitwig and other forks:
- Base rhbitwig had mute/solo layers triggered via MUTE buttons; Drum button was retrig.
- mcristi repurposed Drum/Perform for mute/solo modes and mapped User2 to sampler macros and remote pages (User2/User2-Shift controlling first device params with Pattern Up/Down bank switch)

Implementation notes for the next Akai Fire evolution:
- Target Bitwig controller API 25 so the Fire note mode can investigate Bitwig 6 scale-follow cleanly.
- Remove the "Second Row" preference and standardize the Drum layout to clips on row 1, drum slots on row 2, and steps on rows 3-4.
- Move clip launch quantization to extension preferences so NOTE can become a top-level Note mode selector.
- Treat DRUM as a top-level mode selector that can also cycle through drum sub-modes when pressed again while Drum mode is already active. Use button color to indicate the active drum sub-mode.
- Simplify Euclid to length and pulses only. Remove rotate and invert from the active surface workflow.
- Apply Euclid immediately when length or pulse values change. Clear and rewrite the target lane only when the effective Euclid values actually change, to avoid duplicate note creation and unnecessary clip churn.
- Keep grid-button shifting as the practical replacement for Euclid rotation.
- Rework Accent so it can edit existing notes in place instead of forcing delete-and-reenter behavior.
