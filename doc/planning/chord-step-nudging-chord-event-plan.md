# Chord-Step Nudging Chord-Event Plan

## Problem

The current chord-step nudging logic still behaves like a note-body editor in
some cases, instead of a chord-start editor.

The clearest failure is that longer notes can be coarse-nudged by different
amounts than shorter notes, even when they start on the same chord step.

Examples seen in the current branch:

- a longer note can move by half its own length instead of by one grid step
- notes with different durations in the same chord can move by different
  amounts
- recorded or drawn chord clips with slightly different note lengths can stop
  behaving like a single chord event

That is the wrong model. Nudging should be based on note starts and the current
grid resolution, not on note duration or note body shape.

## Core Requirement

Chord-step nudging must move a chord event by its shared start anchor.

That means:

- coarse nudge moves the chord event by exactly one current grid step
- fine nudge moves the chord event by exactly one fine subdivision
- note duration is preserved, but does not change the movement amount
- all notes in the chord event move together from the same anchor

In short: movement is determined by the chord start, not by individual note
length.

## Why The Current Model Breaks

The current implementation still leans on a step-oriented cache model that
works best when clips are authored from the controller and fit a simple
"one clean chord per step" shape.

That model becomes unreliable when:

- notes in the same chord have different lengths
- notes overlap across steps
- clips were recorded or drawn in Bitwig instead of authored from the
  controller
- a chord spans multiple steps

At that point, the code is no longer operating on a stable "chord event". It is
operating on a set of individual notes whose starts and bodies are inferred
through step observers and cache state.

## Recommended Design Shift

Introduce an explicit chord-event model for nudging.

Instead of treating visible step occupancy as the primary movement target, build
an intermediate representation that groups notes by shared start time.

Each chord event should contain:

- a shared start anchor
- the notes that belong to that anchor
- each note's original duration
- each note's velocity and pitch
- the source step/page metadata needed for controller interaction

Movement should then be expressed as:

- new start = old start + grid delta
- keep each note's own duration unchanged
- rewrite the event at the new anchor

## Proposed Chord-Event Rules

### Supported Cleanly

- grid-aligned chord starts
- notes that share the same start, even if their durations differ
- simple existing clips drawn or recorded in Bitwig, as long as they still map
  to a meaningful shared chord start
- controller-authored chord clips

### Supported With Tolerance

Some recorded material may not have perfectly identical note starts.

To keep that practical, use a small start-time tolerance when grouping notes
into one chord event. The tolerance should be small enough not to merge
neighboring rhythmic events, but large enough to treat ordinary humanized chord
attacks as one event.

This tolerance should be designed explicitly, not emerge accidentally from cache
behavior.

### Out Of Scope Or Rejected

- dense overlapping piano-roll material that does not map to one chord event
- multiple separate note starts packed into the same apparent step region
- clips where grouping is ambiguous enough that a nudge could reasonably move
  more than one musical event

In those cases the controller should either:

- refuse to nudge and show a clear message, or
- only operate on unambiguous grouped events

The system should not guess silently.

## Recommended Architecture

### 1. Separate Observation From Movement

Keep clip observation for UI state and visible occupancy, but stop using raw
observed step occupancy as the movement model.

Add a chord-event extraction stage that builds movement targets from observed
note starts.

### 2. Group By Start Anchor

Build an event list from the observed notes:

- collect note starts
- group notes that share the same start anchor, with a defined tolerance if
  needed
- retain per-note duration and velocity

This event list becomes the source of truth for nudging.

### 3. Move Events, Not Individual Step Fragments

For coarse or fine nudge:

- pick the target chord event(s)
- calculate the new anchor from grid/fine delta
- move all notes in the event to the new anchor
- preserve duration exactly

The movement amount must come only from:

- current grid resolution for coarse nudge
- fine subdivision for fine nudge

It must not come from:

- note length
- note body extent
- cached continuation cells

### 4. Keep Wrap Logic Event-Based

Loop wrap should also be expressed on the event anchor:

- wrap the shared start anchor
- then rewrite the whole event at the wrapped anchor

Do not infer wrap behavior from continuation cells or partial occupancy.

### 5. Add Ambiguity Detection

Before moving a chord event, detect cases where the clip does not map cleanly to
the chord-step model.

Examples:

- two starts within the same grouping window
- overlapping material that would make event selection unclear
- a controller step whose notes belong to multiple musical events

In those cases, do not apply a best-effort move silently. Warn and abort.

## Staged Plan

### Phase 1: Add Chord-Event Extraction

Implement a read-only event model that can be built from the current observed
clip data.

Goals:

- prove that existing clips can be grouped by shared start
- log or inspect grouping results without changing movement yet
- verify that notes with different lengths but same start end up in one event

### Phase 2: Use Event Model For Target Selection

Replace the current "started steps" movement target selection with chord-event
selection.

Goals:

- visible page selects chord events by start anchor
- held-step and shift-all modes target grouped events, not note fragments

### Phase 3: Rewrite Coarse Nudge Around Event Anchors

This is the first priority, because the current bug is especially obvious here.

Goals:

- one coarse nudge = one grid step, always
- longer notes do not move farther than shorter notes
- notes in the same chord event move together, even with different durations

### Phase 4: Rewrite Fine Nudge Around Event Anchors

Once coarse movement is stable, apply the same model to fine movement.

Goals:

- one fine nudge = one fine subdivision, always
- preserve the previous left-edge wrap fixes
- keep wrapped events controllable after crossing the loop boundary

### Phase 5: Add Explicit Unsupported-Clip Handling

When grouping fails or is ambiguous:

- show a controller message such as `Complex clip`
- refuse the nudge rather than corrupting note placement

This is better than silently moving the wrong notes.

## Why This Is Better Than More Cache Patches

The current bug is not just a cache freshness issue.

The most important remaining failure is conceptual:

- coarse nudge amount is being influenced by note duration

That means the movement model itself is wrong for longer notes.

More cache tuning may improve detection and consistency, but it will not solve
the core problem unless movement is anchored to chord starts instead of note
bodies.

## Suggested Implementation Notes

- keep the existing cache and resync fixes that improved observation stability
- do not throw away the current wrap and clip-capacity guards
- add the new event model alongside the current logic first
- validate grouping on existing clips before switching movement over
- prefer small, reversible slices rather than another global movement rewrite

## Test Plan

### Baseline Controller Clips

- simple controller-authored chord clip with equal note lengths
- coarse nudge moves by one grid step
- fine nudge moves by one fine step

### Mixed Duration Chords

- one chord with different note lengths but same start
- coarse nudge moves all starts by the same one-step amount
- fine nudge moves all starts by the same one-fine-step amount
- durations are preserved exactly

### Existing Bitwig Clips

- drawn clip with grid-aligned chord starts and differing note lengths
- recorded clip with small humanized differences in note start
- verify grouping behavior with chosen tolerance

### Wrap Cases

- first-step chord nudged earlier across loop start
- last-step chord nudged later across loop end
- wrapped event remains controllable on the next nudge

### Ambiguity Cases

- overlapping clip with multiple starts in one step region
- verify the controller warns and refuses the move instead of guessing

## Immediate Follow-Up Target

The first acceptance criterion for the advanced fix should be simple and strict:

- a chord whose notes have different lengths, but the same start, must coarse
  nudge by exactly one grid step

Until that is true, the nudging model is still anchored to note length instead
of chord start.
