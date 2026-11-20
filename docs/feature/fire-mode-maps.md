# Akai Fire Mode Maps

## Summary

One printable map per Akai Fire mode: a drawing of the Fire panel with callouts on leader lines saying what each control does in that mode, plus a short "how to get there", an intro, and a few notes under the drawing. Encoder callouts carry one line per encoder page (Channel, Mixer, User 1, User 2); any callout can carry modifier lines such as `SHIFT: fine nudge`.

The maps are published on the oikoaudio website and as a PDF next to GitHub releases. They are not bundled in the in-app help.

## Goal

1. A user opens the website manual or the release PDF.
2. They pick the mode they are in.
3. They see the Fire panel with that mode's controls marked and labelled, the button that enters the mode in its own colour, and the pad areas bracketed.

## Acceptance Criteria

- `just fire-maps` writes `build/fire-maps/fire-mode-maps.html` and, when Chromium is available, `fire-mode-maps.pdf`.
- The PDF has one landscape page per mode.
- The HTML page works in light and dark themes; the SVG carries class names only and the CSS sets every colour.
- First pass contains `Note` and `Drum XOX`.
- Labels live in `tools/fire-maps/modes.toml`, written from `docs/user-guide.md`. Every mode names the user-guide heading it is taken from, and the tests fail if that heading is missing.
- Each control has a fixed callout slot, so a control is labelled in the same place on every map.
- Tests fail when callout text boxes overlap each other or the panel, or when leader lines cross text or each other.

## Data and Layout

- `tools/fire-maps/fire_maps.py`: panel geometry, panel drawing, callout slots, layout lint.
- `tools/fire-maps/modes.toml`: per-mode labels, pad regions, notes.
- `tools/fire-maps/make_fire_maps.py`: writes the HTML page and the PDF.
- `tools/fire-maps/maps.css`: page and SVG styling, light/dark/print.

Colour roles:

| Role | Use |
| --- | --- |
| mode (green) | Controls whose job is specific to this mode |
| pads (orange) | Pad regions and their brackets |
| entry (blue) | The button that enters the mode, with what pressing it again does |
| shared (grey) | Transport, same in every mode |

Callout lines use `TAG: text` for modifiers or held gestures; the tag renders small and muted. Backticks in notes render as bold control names, matching the user guide.

## Scope

Pass 1 (done): generator, HTML, PDF, `Note`, `Drum XOX`.

Pass 2 (done): `Launcher (PERFORM)`, `Multiclip Seq`, `Poly Step`.

Pass 3 (done): `Nested Rhythm`, `Melo Gen`, `Fugue`.

Optional: `Drum Pads` and a shared-controls overview.

Website: `make_fire_maps.py OUTDIR --web WEB_DIR` writes `app/manuals/oikontrol/maps/maps-data.ts` for the oikoaudio site.

Later: attach the PDF to releases from the release workflow.

Out of scope: deriving labels from Java mapping code; in-app help.

## Testing Strategy

- `python3 -m unittest` in `tools/fire-maps/`: data loads, control names are known, guide headings exist, layout lint passes for every mode.
- Manual: open the HTML in light and dark, print the PDF, compare against the controller.

## Risks

- Labels drift from the user guide. Mitigation: guide heading check now; review maps when a mode's user-guide section changes.
- Hand-placed slots stop fitting a mode with longer text. Mitigation: layout lint catches it; shorten text or add a slot variant.
