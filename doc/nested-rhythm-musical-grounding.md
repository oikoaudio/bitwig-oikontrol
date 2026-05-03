# Nested Rhythm Musical Grounding

## Purpose

This note records the musical assumptions that should guide further work on
`Nested Rhythm`. It is not a feature spec for one implementation pass. It is a
grounding document for target order, density thinning, clustering, velocity
contours, and future phrase-shaping changes.

Core tenet:

> Density must thin rhythm without destroying phrase identity.

Sparse settings should not become a pepper-spray of individually important
notes. The beat can be carried by other tracks, but this mode still needs to
preserve its own recognizable timing relationships: anchors, pickups, local
ratchet/tuplet gestures, cadential cells, and the contour that makes those
events feel like one phrase.

## What This Mode Is

`Nested Rhythm` is a structural rhythm generator for drum material. It should
feel like an instrument for shaping phrase grammar:

- meter-aware anchors
- tuplet regions as larger structural divisions
- ratchets as local subdivision of selected parent regions
- clustering as phrase concentration, not randomization
- density as phrase simplification, not isolated-note selection
- velocity contours as part of musical expression, not decorative variation

The mode should remain deterministic and controller-friendly. It should not
grow a "rhythm tradition" selector. The references below should inform the
default behavior, not become style presets.

## Barlow As The Metric Prior

Clarence Barlow's metric indispensability is the closest theoretical starting
point for this mode. In Barlow's terms, pulse positions in a meter have unequal
importance: some are more indispensable for making the meter legible.

Useful adaptation:

- compute or approximate a metric weight for candidate regions
- use that weight to rank anchors, ratchet targets, tuplet targets, density
  survival, velocity emphasis, and phrase landings
- preserve continuity from low to high density, so the rhythm thins by removing
  less necessary material before structurally defining material

Important limitation:

- Barlow ranking alone can still produce sparse isolated dots if every note is
  selected independently
- therefore Barlow should score phrase cells and structural regions, not just
  individual hits

Local reference:

- `ref/Barlowgen-main/README.md` describes indispensability as pulse importance
  in a metric structure.
- `ref/Barlowgen-main/barlowgen.html` contains a compact recursive
  implementation in `barlowSorted(...)` and `barlowIndis(...)`.

External references:

- Clarence Barlow, "On the Quantification of Harmony and Metre"
  https://www.mat.ucsb.edu/Publications/Quantification_of_Harmony_and_Metre.pdf
- Clarence Barlow, "Algorithmic Composition, illustrated by my own work"
  https://www.mat.ucsb.edu/Publications/Algorithmic_Composition.pdf

## Phrase Identity Survives Density

Density should operate on two levels:

1. Choose which phrase cells or structural regions survive.
2. Choose how much internal detail survives inside each selected cell.

This is different from sorting every generated hit by priority and keeping the
top `N`.

Required behavior:

- low density should keep fewer gestures, not evenly scattered singleton notes
- selected gestures should usually keep a lead plus one or more characteristic
  neighboring hits
- ratchet and tuplet interiors should not all have the same priority; some
  interior hits are more phrase-defining than others
- density must remain monotonic: turning density up should add material without
  causing previously retained identity cells to disappear
- clustering may make anchors less sacred, but it should not flatten the whole
  phrase into all-big-hits

The practical selector should therefore rank gesture bundles before final note
selection. A useful shape is:

```text
candidate regions/cells
  -> selected phrase identity cells
  -> retained notes inside selected cells
```

Barlow-style indispensability can help score the cells, but local continuity
and compactness should decide whether a cell remains recognizable.

## Anchors Are Necessary But Not Sufficient

Anchors keep the listener oriented, especially when this clip plays with other
tracks. But a rhythm's identity also lives in the timing between anchors:

- a pickup into a strong point
- a ratchet burst that implies a local pulse
- a tuplet answer across a larger span
- a clustered cadence that creates a compressed phrase ending
- repeated relationships that make a pattern recognizable across density
  changes

At very low density, the mode may collapse toward anchors and a small signature
cell. That is acceptable. But the default thinning strategy should not treat
anchors as the only meaningful residue of the phrase.

## Tuplets, Ratchets, And Structural Order

The structural order should remain:

```text
meter / anchors -> tuplets -> ratchets -> density -> cluster -> velocity shape
```

Tuplets create larger alternate divisions. Ratchets decorate or split selected
parent regions after tuplets exist. This avoids ratchets and tuplets competing
for the same fixed beat grid.

Target order should be phrase-aware:

- one-bar behavior may keep a compact, playable musical order
- multi-bar behavior should rank the whole clip as one phrase, not repeat a
  one-bar recipe in each bar
- target selection should account for metric weight, anticipation into stronger
  points, cadential placement, and local contrast

## Euclidean Geometry Is A Boundary, Not The Core

Godfried Toussaint's work is relevant as a reference for onset geometry and why
evenly distributed timelines recur in many traditions. But `Nested Rhythm`
should not become another Euclidean generator. The project already has
Euclidean material elsewhere, and this mode's value is nested phrase shaping:
tuplets, ratchets, clustering, contour, and density-aware gesture survival.

External references:

- Godfried Toussaint, "The Euclidean Algorithm Generates Traditional Musical
  Rhythms"
  https://www.interaliamag.org/articles/godfried-toussaint-the-euclidean-algorithm-generates-traditional-musical-rhythms/
- Godfried Toussaint, The Geometry of Musical Rhythm
  https://www.routledge.com/The-Geometry-of-Musical-Rhythm-What-Makes-a-Good-Rhythm-Good-Second-Edition/Toussaint/p/book/9780815370970

Use this material only as a boundary and occasional comparison:

- avoid bland maximal evenness as the primary distribution rule
- allow asymmetry, pickups, and phrase endings to override even spacing
- prefer cells that remain identifiable when thinned

## Cultural References Should Inform Abstractions, Not Labels

Several theoretical traditions point toward useful abstractions, but the mode
should not claim to generate named cultural styles.

Agawu and Arom are useful reminders that rhythm is not just additive arithmetic.
It is embedded in language, dance, interlocking parts, repetition, and social
context. For this mode, the practical lesson is modest: preserve phrase
identity and interlocking timing relationships rather than reducing rhythm to a
flat pulse-selection problem.

Carnatic kanakku and konnakkol practice suggest another useful abstraction:
phrases can be built backward from a landing point by arithmetic cells that
resolve exactly. That may be valuable later for cadential endings, but should
not be exposed as a tradition selector.

Jo-ha-kyu is a useful macro-shape metaphor: establish, open/intensify, then
compress or resolve. In this mode that maps naturally onto cluster, density,
ratchet activity, and velocity contour over multi-bar clips.

External references:

- Kofi Agawu, African Rhythm: A Northern Ewe Perspective
  https://www.cambridge.org/9780521480840
- Simha Arom, African Polyphony and Polyrhythm
  https://www.cambridge.org/core/books/african-polyphony-and-polyrhythm/831AF8A34F15E39DC8E8F42104B46EB3
- David P. Nelson, Konnakkol Manual
  https://www.weslpress.org/9780819578785/konnakkol-manual/

## Implementation Implications

The next density/target-order work should move toward this model:

```text
region score =
  metric indispensability
  + anticipation into stronger metric regions
  + phrase/cadence position
  + continuity with already selected cells
  - overcrowding in one neighborhood
```

Then:

```text
density selects cells first
cell retention selects notes second
velocity contour remains attached to structural phrase position
cluster moves/concentrates the retained phrase without erasing identity
```

Acceptance checks for future changes:

- At low density, the result should still contain a recognizable local gesture
  when tuplets or ratchets are active.
- A medium-density mixed tuplet/ratchet pattern should not keep only one family
  unless that is clearly the most phrase-defining material.
- Multi-bar target order should visibly use the whole clip as one phrase.
- Clustering should concentrate the phrase but not make every retained hit sound
  like an equally strong anchor.
- Velocity contours should continue to breathe across meaningful spans such as
  beats, half-bars, and selected phrase regions.

## Open Design Notes

- A future abstract bias control might be useful, but it should describe
  musical behavior directly, for example `Grounded`, `Sync`, `Cadence`, or
  `Phrase`, not a named tradition.
- The local `ref/tessella` material is currently image-based. Treat it as a
  visual/audible comparison point rather than an implementation source.
- If a future implementation adds randomness, it must be seeded and repeatable
  by default. Current nested-rhythm behavior should remain deterministic.
