## Akai Fire (Oiko) – delta vs. upstream rhbitwig

- Packaging: separate Gradle module (`modules/akai-fire`), new UUID `c1f8d20a-3da4-4d2c-8ce1-8b2aa6a8e5b6`, name “Akai Fire by Oiko Audio”.
- Nudge: Grid+arrow buttons left/right for coarse 16th shift of the selected `pad`. Shift+Grid = fine nudge of the selected pad. Held-step micro-nudge disabled for stability for now. 
- Euclid mode: User2 encoders = LEN/PULS/ROT/INV, Browser = apply, Shift=preview placeholder; patterns tile across clip length.
- Play button: Alt+Play retriggers current clip; regular Play toggles transport and retriggers on start.

Notes on behavior in rhbitwig and other forks:
- Base rhbitwig had mute/solo layers triggered via MUTE buttons; Drum button was retrig.
- mcristi repurposed Drum/Perform for mute/solo modes and mapped User2 to sampler macros and remote pages (User2/User2-Shift controlling first device params with Pattern Up/Down bank switch)
