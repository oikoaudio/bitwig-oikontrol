## Akai Fire (Oiko) – delta vs. upstream rhbitwig

- Packaging: separate Gradle module (`modules/akai-fire`), new UUID `c1f8d20a-3da4-4d2c-8ce1-8b2aa6a8e5b6`, name “Akai Fire by Oiko Audio”.
- Nudge: held-step micro-nudge disabled for stability; Grid = coarse 16th shift on selected pad; Shift+Grid = fine nudge on selected pad.
- Euclid mode: User2 encoders = LEN/PULS/ROT/INV (gentler step size), Browser = apply (Alt=clear+apply, Shift=preview placeholder); patterns tile across clip length; gate uses grid*0.48 with a small floor.
- User2 remotes: hardcoded sampler macro modes removed; currently Euclid-only on User2. (Original fork had User2/User2-Shift controlling first device params with Pattern Up/Down bank switch.)
- OLED: value-first Euclid readouts; encoder info strings updated.
- Play button: Alt+Play retriggers current clip; regular Play toggles transport and retriggers on start.
- Micro fixes: ensured Euclid writes always clear lane first; positive gate to avoid insertDuration errors; encoder step sizes reduced for Euclid.
- What’s removed/not used: mute/solo pad layers (available in upstream/mcristi), retrig on Drum button.
- Kept: original Select/Mute top-left button behavior (select vs mute pads via shift toggle), not the mcristi move onto Drum/Perform.

Notes on original behavior:
- Base rhbitwig had mute/solo layers triggered via MUTE buttons; Drum button was retrig.
- mcristimod repurposed Drum/Perform for mute/solo modes and mapped User2 to sampler macros/remote pages.
