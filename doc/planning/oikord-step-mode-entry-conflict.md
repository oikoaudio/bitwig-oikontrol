# Oikord Step Mode Entry Conflict

## Problem

The current route into `Oikord Step` conflicts with established Drum behavior.

In Drum mode:

- `STEP SEQ` is already used for Accent
- `SHIFT + STEP SEQ` is already used for Fill

That makes `STEP SEQ` a poor long-term entry path for `Oikord Step`, because
the button is already carrying important Drum-specific behavior.

## Why This Matters

- mode entry should be reachable from any top-level mode without colliding with
  the primary function of the current mode
- `Oikord Step` is conceptually closer to `NOTE` than to Drum Accent / Fill
- as more modes are added, button ownership needs to remain legible

## Likely Direction

Move `Oikord Step` entry under the `NOTE` button instead of `STEP SEQ`.

Most likely options:

- make `Oikord Step` another value in the `NOTE`-button cycle
- use `ALT + NOTE` as a direct shortcut into `Oikord Step`

## Current Preference

The most coherent default is probably:

- `NOTE` cycles live-note layouts and note-step entry states
- Drum keeps exclusive ownership of `STEP SEQ` for Accent / Fill

An `ALT + NOTE` shortcut may still be useful even if `NOTE`-cycling becomes the
default entry path.

## Deferred Decision

This should be resolved as part of the remaining `PERFORM` / mode-navigation
cleanup, not folded into the encoder-alignment note.
