# Nested Rhythm Follow-up Plan

## Summary

`Nested Rhythm` is now good enough to treat as a real mode rather than an experiment, but the next work needs to be disciplined.

The mode concept is now:

- a dedicated second `DRUM` surface, not a hidden `STEP SEQ` branch
- structural generation built from `Tuplet` and `Ratchet`
- deterministic dynamic shaping with rotateable velocity contours
- no primary `Fill` control

The next steps should focus on:

- cleaning up surface redundancy and broken controls
- extending clip length beyond one bar
- sourcing meter from Bitwig transport instead of inventing controller-side meter controls
- deferring full non-`4/4` generalization until the one-bar assumptions are properly removed

## Current State

Implemented and now considered baseline:

- `Nested Rhythm` is entered as the second `DRUM` surface by pressing `DRUM` again while already in drum mode
- standard drum auto-pin does not carry into `Nested Rhythm`
- `Fill` is no longer part of the nested-rhythm control concept
- `Ratchet > Tuplet > Base` ownership is in place
- `Tuplet` still works over half-bar spans
- velocity rotation now rotates final assigned velocities by emitted-hit order
- velocity shaping now uses `Velocity Center` plus `Velocity Depth` as an offset multiplier
- `Velocity Center` is on `ALT + Mixer Encoder 1`

Still unsatisfactory or unresolved:

- `Hit Vel`, `Hit Gate`, and `Hit Select` are duplicated across `Mixer` and `User 1`
- `Hit Gate` still does not appear to change actual written note lengths in Bitwig and must be treated as a bug
- the mode is still hard-coded to one bar
- the generator is still structurally hard-coded to `4/4`
- `Root` and `Octave` are still split even though this mode would benefit from a single linear pitch control

## Locked Decisions

- Do not reintroduce `Fill` as a primary control.
- Do not add manual meter controls on the controller.
- Read meter from Bitwig transport time signature when meter support is implemented.
- Clip/play length should be implemented before full arbitrary-meter generalization.
- `Nested Rhythm` should remain a separate drum-family surface, not a `STEP SEQ` entry path.
- Additional expression layers are desirable, but can wait until core timing and layout issues are solved.

## Next Implementation Order

### Pass 1: Surface Cleanup And Broken Controls

Goals:

- remove obvious redundancy
- fix controls that do not match the written clip
- prepare encoder space for future expression work

Tasks:

- remove duplicated hit-edit controls from `Mixer`
  - keep `User 1` as the dedicated per-hit edit page
  - reserve `Mixer` for mode-level dynamics and future expression layers
- audit and fix `Hit Gate`
  - verify what unit `cursorClip.setStep(...)` expects for note duration in this fine-grid setup
  - confirm whether written durations in the clip actually change
  - if the write path is wrong, fix it
  - if the write path is correct but Bitwig normalizes it in this setup, either redesign the feature or remove it
- clarify `User 2 / Encoder 3`
  - it currently resets local hit edits only
  - improve label if kept
- fold `Root` and `Octave` into one linear non-wrapping pitch control for this mode
  - keep the implementation compatible with the shared pitch context if possible
  - if not possible without cross-mode side effects, document the reason and defer

Acceptance criteria:

- `Mixer` no longer duplicates `User 1`
- `Hit Gate` either visibly changes note lengths in the Bitwig clip or is removed/repurposed
- `User 2` layout is easier to justify from the controller alone

### Pass 2: Clip Length Beyond One Bar

Goals:

- let the mode generate over more than one bar without tackling full meter generalization yet

Tasks:

- add a clip/play-length parameter
  - likely `1`, `2`, `4` bars first
- stop forcing clip loop length to a single bar on every write
- generalize event generation over multiple bars while still assuming `4/4`
- decide how the projected pad view behaves when the generated hit count exceeds the visible range
  - likely paging later, but generation should not depend on paging existing
- make sure entering `Nested Rhythm` does not write anything until the user actually generates or edits parameters

Acceptance criteria:

- generated clips can be longer than one bar
- structural generation works across the full selected clip length
- one-bar behavior remains intact when play length is `1`

### Pass 3: Meter From Transport

Goals:

- make the generator follow Bitwig’s current time signature
- avoid inventing controller-local meter state

Tasks:

- observe `transport.timeSignature()` from the Bitwig API
- read numerator and denominator from transport
- thread the current transport meter into nested-rhythm generation and clip writing
- replace hard-coded `4/4` assumptions in:
  - anchor placement
  - half-span coverage logic
  - ratchet width ordering
  - bar-length and fine-step calculations
  - projected pad/bin visualization

Constraints:

- do not add separate controller-side meter parameters
- use Bitwig transport as the source of truth

Acceptance criteria:

- switching transport meter in Bitwig changes nested-rhythm generation accordingly
- controller labels and behavior remain coherent without extra meter controls

### Pass 4: Expression Layer Expansion

This is intentionally deferred until timing and layout are stable.

Candidate directions:

- additional expression contours beyond velocity
- release velocity contour and rotation
- contour-controlled per-hit expression lanes that suit Bitwig note-expression-friendly instruments

Design rule:

- new expression layers should behave like velocity does now:
  - deterministic
  - contour-based
  - rotateable by emitted hit order
  - clearly separate from structural timing generation

## Technical Notes

### Meter Source

Bitwig API exposes transport time signature directly, so meter should come from:

- `transport.timeSignature()`
- `numerator()`
- `denominator()`

That is the right long-term source of truth.

### Why Meter Is Still Deferred

Reading the meter is easy. Generalizing the mode is the hard part because the current code still assumes:

- four beats per bar
- one bar only
- half-bar logic tied to `4/4`
- ratchet ordering over exactly four beats
- fixed projected bar structure

So the meter decision is solved conceptually, but not implemented yet.

### Hit Gate

Given the latest report, `Hit Gate` should now be treated as a confirmed bug until proven otherwise. “Seems musically negligible” is no longer an adequate explanation because the actual note lengths in the Bitwig clip were inspected and did not change.

## Suggested File Touches

Likely next files:

- [NestedRhythmMode.java](/home/davfre/d/prj/df/audio/bitwig/bitwig-oikontrol/modules/akai-fire/src/main/java/com/oikoaudio/fire/nestedrhythm/NestedRhythmMode.java:1)
- [NestedRhythmGenerator.java](/home/davfre/d/prj/df/audio/bitwig/bitwig-oikontrol/modules/akai-fire/src/main/java/com/oikoaudio/fire/nestedrhythm/NestedRhythmGenerator.java:1)
- [AkaiFireOikontrolExtension.java](/home/davfre/d/prj/df/audio/bitwig/bitwig-oikontrol/modules/akai-fire/src/main/java/com/oikoaudio/fire/AkaiFireOikontrolExtension.java:1)
- [NestedRhythmGeneratorTest.java](/home/davfre/d/prj/df/audio/bitwig/bitwig-oikontrol/modules/akai-fire/src/test/java/com/oikoaudio/fire/nestedrhythm/NestedRhythmGeneratorTest.java:1)
- [doc/user-guide.md](/home/davfre/d/prj/df/audio/bitwig/bitwig-oikontrol/doc/user-guide.md:332)
- [doc/akai-fire-controller-layout.md](/home/davfre/d/prj/df/audio/bitwig/bitwig-oikontrol/doc/akai-fire-controller-layout.md:334)
- [rhythm-step-generator-spec.md](/home/davfre/d/prj/df/audio/bitwig/bitwig-oikontrol/doc/dev/feature/rhythm-step-generator-spec.md:223)

## Testing Strategy

### Immediate

- verify `Hit Gate` by inspecting actual written note lengths in Bitwig
- confirm `DRUM -> Nested Rhythm -> DRUM` transitions do not force the wrong pin context
- confirm no generation occurs merely by entering the nested surface
- verify velocity center/depth still behave correctly after any control-map cleanup

### After Clip Length Work

- one-bar, two-bar, and four-bar generation all write valid clip lengths
- generated hit ordering and rotation still work across longer loops
- bottom-row selection remains coherent even if more hits exist than can be displayed at once

### After Meter Work

- transport meter changes are reflected without controller-side meter controls
- `3/4`, `5/4`, and `7/8` style smoke tests produce sensible anchor structures
- tuplet and ratchet coverage still behave predictably in non-`4/4`

## Success Criteria

- the next work removes obvious surface duplication
- `Hit Gate` is either fixed or gone
- clip length becomes variable before full meter generalization
- meter comes from Bitwig transport rather than controller-local state
- the mode remains conceptually clean as a deterministic nested-rhythm generator rather than drifting back toward generic step-sequencer behavior
