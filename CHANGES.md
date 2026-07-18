# Change log

This document now tracks intentional modifications made to the `bitwig-oikontrol` project.

## Unreleased

- Added Akai Fire Multiclip Seq as a four-row `DRUM` surface for sequencing up to sixteen positional child-track clips with independent lengths/play starts, lane and time paging, scene population, Track mute/solo, and independent playheads.
- Fixed Multiclip Seq retargeting so existing clip notes hydrate into the pad display and writes cannot fall through to a previously selected scene; row LEDs now show bright-audible/dim-muted state, with `ALT + MUTE_n` reserved for lane selection.
- Fixed Multiclip Seq Lane Clip cursors remaining attached to the previous globally selected clip by waiting for each requested scene slot to settle before independently pinning its observation views and enabling edits.
- Multiclip Scene activation now populates only eligible child-track slots, launches those child clips only with the explicit Shift gesture, never launches or selects the aggregate group scene/sub scene, and derives overlay feedback exclusively from child-track clips.
- Simplified Multiclip Seq to one active Lane Clip cursor: exact child-bank clip creation no longer auto-selects clips, touching another row switches once and queues that edit, inactive row observers are cleared, and cross-row pad input is blocked while another row is held or the cursor is settling.
- Note Repeat divisions are now ordered monotonically from fastest to slowest, interleaving triplet and straight rates while retaining `1/16` as the default.
- In live Note, Harmonic, and Drum Pads input, hold `MUTE_3` and turn `SELECT` to adjust the remembered Note Repeat division without toggling repeat; an unmodified `MUTE_3` tap still toggles it.
- Live Note and Harmonic pads now use per-note ownership, so overlapping pads play their combined note union and shared notes remain sounding until the last owning pad is released, including with Hold mode.
- Harmonic input Mixer encoder 4 now selects Drop 2, Close, or Open voicings and safely retunes held pads; Pitch Gliss remains on Channel encoder 3.
- Melo Gen now uses `SHIFT + Channel encoder 3` as a shared-scale shortcut, matching Poly Step's `SHIFT` scale and `ALT` root modifier grammar on its pitch-context encoder.
- Rolling generator density now grows monotonically from four characteristic family hits per 16 steps at `0.00` to every step at `1.00`, while keeping the seeded family stable across density changes.
- Acid generator density now spans the full displayed range, growing deterministically from a sparse metric backbone at `0.00` to the complete seeded rhythm skeleton at `1.00`.
- Fugue now enters source-only clips with derived lines 2-4 off, protects pre-existing channel 2-4 notes from controller regeneration, and requires `PATTERN DOWN` to explicitly replace and claim those derived channels.
- Added Baked Note Variation across Drum XOX, Poly Step, Melodic Step, Fugue, and Nested Rhythm: `SHIFT + ALT` adjusts and applies stable clip-wide per-note variation, zero amount resets to mode defaults, and a latched `ALT + KNOB MODE` User 1 page exposes Note Gain and Note Pan in compatible step modes.
- Restored Melodic Step density generation and made ALT density thinning/filling follow the shared metric-indispensability ranking instead of clip scan order.
- Shortened Poly Step's paged chord-family OLED labels so the complete page fraction remains visible in the large
  value font.
- Fixed Poly Step pad LEDs temporarily blanking during parameter edits while its fine-grid observation cache was
  being repopulated.
- Poly Step now assigns each finely nudged note or chord to exactly one nearest step pad, transfers that ownership
  at the midpoint, and preserves long-note duration without lighting or clearing neighboring sustain steps.
- Poly Step chord-set pads now support simultaneous multi-pad grips, combining and deduplicating the selected
  chords' pitches for audition, step insertion, held-step assignment, and source-first pitch replacement.
- Poly Step User 1 and User 2 encoders now edit session-local insertion defaults when no note is selected; touching
  shows the current default, and `KNOB MODE + touch` restores its original value. New notes receive those velocity,
  duration, probability, expression, velocity-spread, pitch-expression, and repeat defaults.
- Poly Step Channel encoder 3 now shows the selected pitch/chord set and page prominently on touch and turn,
  instead of rendering the current chord in the OLED center and relegating the set name to the encoder header.
- Renamed the user-facing Chord Step workflow to Poly Step and added a source-first replacement gesture: hold one
  or more source pitches/chords, then tap an occupied step to replace only its pitches while preserving timing,
  duration, velocity, probability, expression, repeat, recurrence, occurrence, and other note conditions.
- Fixed Drum XOX pad-pattern copy so destination steps preserve source velocity, duration, probability, and the
  existing copied note-expression parameters instead of receiving fixed velocity and zero probability.
- Refactored Akai Fire Chord Step around direct responsibility owners, workflow-level regression scenarios,
  concrete hardware/main-encoder boundaries, and explicit acyclic encoder-layer initialization across Drum XOX,
  Melodic Step, Chord Step, and Nested Rhythm; controller mappings and musical behavior are unchanged.
- Fixed main-encoder role switching after active Note Repeat and prevented Nested Rhythm startup from querying an
  encoder layout through a partially initialized mode.
- Akai Fire live Note now shows played pad notes/chords on the OLED, with chord detection adapted from Wim van den Borre's BitX.
- Akai Fire live Note now maps `MUTE_4` to Hold, latching only the pad notes/chords already down when Hold turns on; held pads blink until pressed again or Hold is turned off.
- Launch Control XL factory modes now use an eight-track factory bank again and explicitly own Bitwig's controller feedback box, so the red box matches the eight faders/knob columns; Track Left/Right pages by eight tracks and selects the first strip after paging while still honoring the `Show deactivated tracks` preference.
- Akai Fire Perform now uses a flat track bank for launcher and Mix pages, so expanded group children appear in the visible track window.
- `SHIFT + PERFORM` is now presented as the Mix page, and its track-select pads use the corresponding Bitwig track colors.
- The Mix page `MUTE_1`-`MUTE_4` buttons now jump to arrangement boundaries and clear project solo/mute states instead of acting as clip modifiers.
- The Mix page status LEDs near `MUTE_2` and `MUTE_3` now mirror the project solo/mute indicators.
- The Akai Fire Mix page now uses `PATTERN DOWN` / `PATTERN UP` to enter and leave a device-centered view of the first four devices on each visible track, with plain pads selecting devices and showing device names on the OLED, `ALT + pad` toggling bypass, and encoders focusing selected-device remotes while the view is active.
- Mix no longer uses `PATTERN UP/DOWN` for scene scrolling, and launcher orientation labels are now `LauncherV`/`LauncherH`.
- Launcher and Mix OLED idle screens now show visible-track RMS meters as vertical bars, and the Mix page's Mixer encoder page shows selected-track RMS and peak max/current values.
- When playback stops, Akai Fire idle OLED meters now ring out briefly before falling back to the selected track name instead of silent meter displays.
- Drum XOX now shows visible Drum Machine pad-chain RMS meters as vertical bars on the idle OLED, with selected-pad Peak/RMS max/current values on the Mixer encoder page.
- Akai Fire OLED graphics now send only changed 8-pixel pages, allowing meter displays to refresh about every 100 ms without full-frame redraws, and bar meters show a slowly decaying held-peak marker plus a bottom dash for muted lanes.
- Akai Fire OLED information displays now keep compact encoder legends visible across idle, transient, and meter screens, support a top/bottom legend position preference, use clearer short labels, and keep persistent mode/track context visible instead of blanking after mode changes.
- Added a `Playback Start` SELECT encoder role for moving Bitwig's playback start position by the arranger grid resolution.
- Akai Fire global transport navigation now uses `PATTERN + SELECT` for cue markers, `SHIFT + PATTERN + SELECT` for fine play-start moves, launches from the current play start on `PLAY`, and moves play start to arrangement start on `STOP` when already stopped.
- Akai Fire now uses the next `PLAY` after a double-`STOP` arrangement reset to invoke Bitwig's global retrigger for playing Launcher clips before launching from the play start, while plain `PLAY` otherwise preserves clip offsets and explicit retrigger stays on `ALT + PLAY`, clip-row pads, and Perform launcher pads.
- Akai Fire now targets Bitwig API 25 and splits recording/automation controls around Bitwig 6's unified write model: plain `PATTERN` toggles arranger and launcher automation write together, `ALT + REC` toggles arranger overdub, `ALT + PATTERN` toggles launcher overdub and enables touch automation write when needed, and `PATTERN + REC` records the selected track into the next free launcher slot.
- Akai Fire cue-marker navigation now advances by one marker per `SELECT` tick, and the last-touched parameter `SELECT` role uses a faster default speed with `SHIFT` matching the former default speed.
- Added an `Exclusive Track Arm` preference to Akai Fire and Launch Control XL; when disabled, solo/mute/arm actions no longer change track selection, when enabled, arm selects the armed track while disarming the other visible tracks, and Akai Fire `ALT + arm pad` inverts the arm mode for that press.
- Akai Fire now briefly shows the selected track name on the OLED when Bitwig or another controller changes the visible track selection, then returns to the meter display.
- Akai Fire live Note now uses `PATTERN DOWN/UP` for next/previous shared scale, `ALT + PATTERN DOWN/UP` for lower/higher shared root, moves Channel encoder 4 to Timbre CC74, and returns Channel/Mixer pages to selected-track meter idle displays after transient encoder values.
- Akai Fire live Note User 1 encoder 3 now sends Breath CC2 instead of duplicating Timbre.
- Akai Fire live Note and Harmonic modes now have a `SHIFT + STEP` Bitwig Step Input helper that opens the selected clip in the Detail Editor, selects the Step Input tool, and shows an estimated `Step N/M` OLED position.
- Chord Step now starts from an open `Builder` chord source with chromatic or in-key source-pad layouts, optional builder-note latch on `SHIFT + PATTERN DOWN/UP`, and `SHIFT + Channel encoder 4` / `ALT + NOTE` layout switching for building chords directly from the pad rows.
- Launch Control XL now clears LED dirty state after batched sysex flushes and disables verbose MIDI debug logging by default to reduce controller traffic and console load.
- Added an Akai Fire `Startup Mode` preference for starting in Note, Harmony, Drum XOX, Launcher, or Mix.
- `SHIFT + NOTE` now toggles Bitwig record quantization off and back to the previous grid, defaulting to `1/16`.
- `ALT + BANK LEFT/RIGHT` now triggers Bitwig undo/redo in Launcher, Mix, Note, Harmonic, and Drum Pads modes.
- `SHIFT + BROWSER` now latches the global settings overlay, with `KNOB MODE` switching to a second page for shared input velocity feel and pad color response.
- The Fire global settings overlay now has a `Pins` page for clamped track, device, and clip pin on/off controls.
- `REC` can stop launcher recordings started from the controller, including `PATTERN + REC` next-free-slot recording and Perform `REC + pad` recording, even after switching modes.
- Akai Fire now separates fixed `Default Clip Length` for empty clip creation from `Launcher Record Length` for fixed/manual/rounded launcher recording.
- Added an Akai Fire `Screen Message Hold` preference with 750 ms, 1.5 s, and 3 s OLED transient message durations.

## [2.21.0](https://github.com/oikoaudio/bitwig-oikontrol/compare/oikontrol-v2.20.0...oikontrol-v2.21.0) (2026-07-17)


### Features

* **fire:** add baked note variation foundation ([c9301e8](https://github.com/oikoaudio/bitwig-oikontrol/commit/c9301e8a4c6f1e934c31c521da59313a6235201e))
* **fire:** add Drum XOX baked variation ([9c2144d](https://github.com/oikoaudio/bitwig-oikontrol/commit/9c2144d13b4fe345b3caaa9e87af26b61a591732))
* **fire:** add harmonic voicing selector ([1baeb58](https://github.com/oikoaudio/bitwig-oikontrol/commit/1baeb58c2b88acd3353b1a8876273e0bf28bf862))
* **fire:** add held note repeat rate gesture ([3ef297e](https://github.com/oikoaudio/bitwig-oikontrol/commit/3ef297e1994b068ce6887ec01bf9d83a4c726683))
* **fire:** add Melodic and Fugue baked variation ([a8661f4](https://github.com/oikoaudio/bitwig-oikontrol/commit/a8661f4cc10bae2b786c4cb8ede7283f6bf06797))
* **fire:** add melodic scale encoder shortcut ([117d31f](https://github.com/oikoaudio/bitwig-oikontrol/commit/117d31f6f95d27781ae440b54c562bdcb1c1c21f))
* **fire:** add Nested Rhythm baked variation ([0e19bad](https://github.com/oikoaudio/bitwig-oikontrol/commit/0e19bad91ca86444307861f90b61fed87227090c))
* **fire:** add Poly Step baked variation pilot ([7b506d7](https://github.com/oikoaudio/bitwig-oikontrol/commit/7b506d7ccd76fadf53de024eeeff4e155e0db9d2))
* **fire:** add Poly Step insertion defaults ([ba30d86](https://github.com/oikoaudio/bitwig-oikontrol/commit/ba30d8621537f17c5c583aa9834dfcc874083fd0))
* **fire:** add Poly Step pitch replacement gesture ([f09bce6](https://github.com/oikoaudio/bitwig-oikontrol/commit/f09bce6da7f49e35974aee134fe60509c16fdc34))
* **fire:** combine Poly Step chord-set pads ([ca46c75](https://github.com/oikoaudio/bitwig-oikontrol/commit/ca46c751f2a02e3924370e426c3c47493c002b0a))
* **fire:** map note pan and gain variation ([3e6a1ca](https://github.com/oikoaudio/bitwig-oikontrol/commit/3e6a1ca616b477e4a0947a0b679a9d3720ae70ff))
* refine controller architecture and sequencing workflows ([04ffa41](https://github.com/oikoaudio/bitwig-oikontrol/commit/04ffa41eb4ca79bedcea3dad527d8e4283405aaf))


### Bug Fixes

* **akai-fire:** align track select indicator LED feedback ([af5d921](https://github.com/oikoaudio/bitwig-oikontrol/commit/af5d9216fae0d5b54bde8d7ced77be83afe17bff))
* **fire:** align Poly Step fine-note ownership ([4853253](https://github.com/oikoaudio/bitwig-oikontrol/commit/485325363f4e34658d312b08f9bc990e4299af3e))
* **fire:** allow overlapping live pad unions ([a4c0a03](https://github.com/oikoaudio/bitwig-oikontrol/commit/a4c0a034ca80ba07daee2e4cabd2ede318c95a0a))
* **fire:** clarify Poly Step chord set feedback ([6d59034](https://github.com/oikoaudio/bitwig-oikontrol/commit/6d59034f13ddfef343a9383a7d3338e2d754bc08))
* **fire:** fit Poly Step chord page labels ([37c9ed4](https://github.com/oikoaudio/bitwig-oikontrol/commit/37c9ed482d70403e239e308ac77713aba2963cc4))
* **fire:** keep Poly Step pads visible during refresh ([9470cc3](https://github.com/oikoaudio/bitwig-oikontrol/commit/9470cc31b5def543f7d925201070182ceac48bbc))
* **fire:** make acid density span sparse to full ([10a2a1b](https://github.com/oikoaudio/bitwig-oikontrol/commit/10a2a1b5e52aa764338f5cdd673065fa377f8e7b))
* **fire:** make melodic density regenerate existing phrases ([aa2c00c](https://github.com/oikoaudio/bitwig-oikontrol/commit/aa2c00cd5643f8e46a2db19a46c36325291956cd))
* **fire:** order note repeat rates by duration ([b898fe3](https://github.com/oikoaudio/bitwig-oikontrol/commit/b898fe3f7d91ba57eab8bc366b234764bd90b872))
* **fire:** preserve copied drum pattern parameters ([57e29fe](https://github.com/oikoaudio/bitwig-oikontrol/commit/57e29fe60e7d7cd0fcdad1aa5fa5bc54915175e4))
* **fire:** protect fugue derived lines on entry ([922c7aa](https://github.com/oikoaudio/bitwig-oikontrol/commit/922c7aa71647f349c3ea1767d001faf1667f6ba2))
* **fire:** restore encoder routing and safe initialization ([02913b8](https://github.com/oikoaudio/bitwig-oikontrol/commit/02913b8982b1f8bd4b138206fe40227d572fba2c))
* **fire:** restore musical melodic density ([e91eb65](https://github.com/oikoaudio/bitwig-oikontrol/commit/e91eb6512c8464c50e3d38df6edcbcc22e7e17ad))
* **fire:** widen rolling density range ([7f9ffe5](https://github.com/oikoaudio/bitwig-oikontrol/commit/7f9ffe55b1b19a037d39dc09fef2b88fb4d7cb00))

## [2.20.0](https://github.com/oikoaudio/bitwig-oikontrol/compare/oikontrol-v2.19.0...oikontrol-v2.20.0) (2026-07-06)


### Features

* **akai-fire:** add compact OLED mixer value bars ([2f6a96f](https://github.com/oikoaudio/bitwig-oikontrol/commit/2f6a96fe0db4d8b214dce53ade55e2e73e8501cb))
* **akai-fire:** add live note chord OLED and pad hold ([4689087](https://github.com/oikoaudio/bitwig-oikontrol/commit/468908732df8d22cbfc2450ebaee7ac2715acb91))
* **akai-fire:** add note and chord OLED display ([ade85c2](https://github.com/oikoaudio/bitwig-oikontrol/commit/ade85c2d23a81709ed2928db568e5a422c364caf))
* **akai-fire:** map alt encoders to second remote bank ([397c692](https://github.com/oikoaudio/bitwig-oikontrol/commit/397c69256560804598ec8d6dd1674e7719db78ea))


### Bug Fixes

* **akai-fire:** make gliss state visible and resettable ([586fc39](https://github.com/oikoaudio/bitwig-oikontrol/commit/586fc3963c838aac6570fc025fc24dc759126fd9))
* **akai-fire:** tune pan encoder response ([5b1cbf7](https://github.com/oikoaudio/bitwig-oikontrol/commit/5b1cbf7fd25950ba0dcf06b579d9357d3fddcd8f))

## [2.19.0](https://github.com/oikoaudio/bitwig-oikontrol/compare/oikontrol-v2.18.0...oikontrol-v2.19.0) (2026-06-08)


### Features

* add OLED encoder legend position ([59e3265](https://github.com/oikoaudio/bitwig-oikontrol/commit/59e32657de6678ec73e35650a8398053d052cb53))
* **akai-fire:** add Bitwig step input helper ([90570f8](https://github.com/oikoaudio/bitwig-oikontrol/commit/90570f8d14d605f169605ca80341517fa6268bb9))
* **akai-fire:** add chord builder layout control ([63d6430](https://github.com/oikoaudio/bitwig-oikontrol/commit/63d643083eff2d137e284a85292dabd3397c457a))
* **akai-fire:** improve idle OLED context and encoder legends ([70b151b](https://github.com/oikoaudio/bitwig-oikontrol/commit/70b151b956bd1a9abe21e3bfe49e0de69c38d0be))
* **akai-fire:** refine automation and idle OLED workflow ([a17495d](https://github.com/oikoaudio/bitwig-oikontrol/commit/a17495d369719f425358ac12d18bde27b00fb5e9))
* **akai-fire:** remember mode family pages ([ad77f0b](https://github.com/oikoaudio/bitwig-oikontrol/commit/ad77f0b7a23e31cf971b10c0f42e857402d5870d))
* refine chord builder latch and LCXL track paging ([f1365d3](https://github.com/oikoaudio/bitwig-oikontrol/commit/f1365d386c3d8284281485d8448788d592609722))
* retrigger launcher clips after double stop ([35e6b29](https://github.com/oikoaudio/bitwig-oikontrol/commit/35e6b2907f01a3b3551a340a058b505d2fa304c8))


### Bug Fixes

* **akai-fire:** keep contextual OLED idle screens visible ([af8f7d2](https://github.com/oikoaudio/bitwig-oikontrol/commit/af8f7d2e96d3b5aba80d73905e7dee6a878c948e))

## [2.18.0](https://github.com/oikoaudio/bitwig-oikontrol/compare/oikontrol-v2.17.0...oikontrol-v2.18.0) (2026-05-21)


### Features

* refine controller navigation and metering ([3203e67](https://github.com/oikoaudio/bitwig-oikontrol/commit/3203e679615f4bbf645c7505a631a34436d122ea))
* refine controller navigation and metering ([76f0404](https://github.com/oikoaudio/bitwig-oikontrol/commit/76f0404f0431b23e019a9fec1f35d6b2440e4d11))
* refine live note expression controls ([ec2a94c](https://github.com/oikoaudio/bitwig-oikontrol/commit/ec2a94cd2db7169112c9f74be92ed9e275352f23))

## [2.17.0](https://github.com/oikoaudio/bitwig-oikontrol/compare/oikontrol-v2.16.0...oikontrol-v2.17.0) (2026-05-19)


### Features

* add explicit encoder reset defaults ([a26a007](https://github.com/oikoaudio/bitwig-oikontrol/commit/a26a0074e569c62307a57050df06fa2581bdfd1d))
* add knob mode encoder page and reset chords ([a5eb8bd](https://github.com/oikoaudio/bitwig-oikontrol/commit/a5eb8bdca7245e7a81f7cdfa761f3e15a7f5841b))
* add paged perform mix device controls ([cf3925a](https://github.com/oikoaudio/bitwig-oikontrol/commit/cf3925a1e8da0cd29bfdee3c0c5660fc3fbef2b7))
* add perform birds-eye navigation ([a55d36e](https://github.com/oikoaudio/bitwig-oikontrol/commit/a55d36e65d5bbfe00d26be61971f8fa006fc813e))
* add perform device layer mixer page ([ad8597e](https://github.com/oikoaudio/bitwig-oikontrol/commit/ad8597edb921728e7debeb73d9b0c7d2005822c7))
* add perform device row toggles ([64911fd](https://github.com/oikoaudio/bitwig-oikontrol/commit/64911fd9ee8dcb4d33fac9c1fd4d8bc9f1d6cdeb))
* add quick launcher clip recording chord ([f6d4225](https://github.com/oikoaudio/bitwig-oikontrol/commit/f6d4225720d18bcc24826cca5c47d3ef650d87ac))
* add screen message hold preference ([aca60ce](https://github.com/oikoaudio/bitwig-oikontrol/commit/aca60ce38b8b8d87fc75fd3e97d49026123a386f))
* improve Akai Fire perform and launcher workflows ([3e81f7d](https://github.com/oikoaudio/bitwig-oikontrol/commit/3e81f7d6280e6186d6ad3e93f6703ed04da6621a))
* navigate perform device remote pages ([18fe6d4](https://github.com/oikoaudio/bitwig-oikontrol/commit/18fe6d4d10fb444b25c99e51fa164e4436fc7611))
* remember perform mix device selections ([e8d4665](https://github.com/oikoaudio/bitwig-oikontrol/commit/e8d4665a207417a86c8628100dbc3fa65f032f30))
* route browser transport controls ([eb08bda](https://github.com/oikoaudio/bitwig-oikontrol/commit/eb08bda28108d4168e9f019a0f23c425d5b2ac5f))
* split clip create and record length settings ([280e721](https://github.com/oikoaudio/bitwig-oikontrol/commit/280e721b188ceaa6b9fee885648634678cc3fe40))
* toggle perform device windows from grid ([072a10f](https://github.com/oikoaudio/bitwig-oikontrol/commit/072a10f677541a626024e9a6ff0021e21dd666e0))


### Bug Fixes

* improve mix device overview selection feedback ([4c52947](https://github.com/oikoaudio/bitwig-oikontrol/commit/4c529479e5fa29fe95db3df3b2194ee405f4eec5))
* invalidate perform meter oled on suppression ([65b4c77](https://github.com/oikoaudio/bitwig-oikontrol/commit/65b4c77831f6323a24c58b7851521bb866adec21))
* keep transient oled actions readable ([20163d6](https://github.com/oikoaudio/bitwig-oikontrol/commit/20163d63f78f3396e6e9351972ff4f7ca3fe930d))
* let mode buttons exit birds eye ([5d3e04f](https://github.com/oikoaudio/bitwig-oikontrol/commit/5d3e04f383812ce010e1cd82359084dffc0e3b18))
* make quick launcher recording mode-independent ([c2db182](https://github.com/oikoaudio/bitwig-oikontrol/commit/c2db182253518e9824ad159c1b2813a3f01f42ec))
* route track select oled through perform display ([a418cd4](https://github.com/oikoaudio/bitwig-oikontrol/commit/a418cd4e6475a481847014be12ac5a086bc2e6e7))

## [2.16.0](https://github.com/oikoaudio/bitwig-oikontrol/compare/oikontrol-v2.15.0...oikontrol-v2.16.0) (2026-05-18)


### Features

* add Akai Fire Mix device view ([aacef6c](https://github.com/oikoaudio/bitwig-oikontrol/commit/aacef6c70efd571d54a24ae40cc84ee3dec4a8cd))
* add Fire global pin settings ([7d0d9eb](https://github.com/oikoaudio/bitwig-oikontrol/commit/7d0d9eb8324c01855bfe08328c5e90601c5ecbeb))
* add shift select playback-start shortcut ([b0a3a7b](https://github.com/oikoaudio/bitwig-oikontrol/commit/b0a3a7b29dec169d72678795c862273906964f04))
* add undo redo bank gesture ([c3c62f4](https://github.com/oikoaudio/bitwig-oikontrol/commit/c3c62f46c76872db7e5075dd4f3ee0a00c77e237))
* improve Akai Fire global controls ([9481e48](https://github.com/oikoaudio/bitwig-oikontrol/commit/9481e486a5ace0c45245b475c97d5bdabb3cd353))
* latch Fire global settings overlay ([629ef69](https://github.com/oikoaudio/bitwig-oikontrol/commit/629ef69c04ce41fa98f3c888ad1b75c97589fb38))


### Bug Fixes

* tune playback-start grid jumps ([8fef568](https://github.com/oikoaudio/bitwig-oikontrol/commit/8fef5681738ca767f64c8f249c10cc75169b12e6))

## [2.15.0](https://github.com/oikoaudio/bitwig-oikontrol/compare/oikontrol-v2.14.1...oikontrol-v2.15.0) (2026-05-18)


### Features

* add Akai Fire OLED meters ([f91820d](https://github.com/oikoaudio/bitwig-oikontrol/commit/f91820da0eb93b3424a26f6786233a58e4dc1dab))
* add Akai Fire startup mode preference ([d8e385d](https://github.com/oikoaudio/bitwig-oikontrol/commit/d8e385d097c6c2dcc85c6172503d8fdab6e828b5))
* improve Akai Fire Mix and launcher controls ([3d4e184](https://github.com/oikoaudio/bitwig-oikontrol/commit/3d4e1846d029c4db5af48adeddddd846fb296a58))
* improve Akai Fire Mix, Launcher, and OLED meters ([8c5ad43](https://github.com/oikoaudio/bitwig-oikontrol/commit/8c5ad433f8f53d032f8da26ee3d44bc8f2f515b7))
* toggle Akai Fire record quantization ([de5823a](https://github.com/oikoaudio/bitwig-oikontrol/commit/de5823a44d55bea8de1e1efbbaa2491f5d2a9faa))


### Bug Fixes

* align Drum mixer OLED meters ([a1fd662](https://github.com/oikoaudio/bitwig-oikontrol/commit/a1fd662b397e78890d08729e6bd76cc10ccc12cb))


### Performance Improvements

* send Akai Fire OLED graphics by changed page ([828282f](https://github.com/oikoaudio/bitwig-oikontrol/commit/828282fece7e29007e4561255f69e29522120eb3))

## [2.14.1](https://github.com/oikoaudio/bitwig-oikontrol/compare/oikontrol-v2.14.0...oikontrol-v2.14.1) (2026-05-16)


### Bug Fixes

Akai Fire control and documentation polish:

- `STEP` now toggles accent for held step pads in Melodic Step and Chord Step, without requiring `SHIFT`.
- Drum Pads now use `PATTERN UP/DOWN` to move through Drum Machine pad pages.
- The user guide and bundled help have been refreshed for current Akai Fire controls, harmonic note input, chord families, melodic generators, Nested Rhythm, and Fugue. ([646372a](https://github.com/oikoaudio/bitwig-oikontrol/commit/646372ade9e15ade6ffbea5422891cc6207ff8bb))

## [2.14.0](https://github.com/oikoaudio/bitwig-oikontrol/compare/oikontrol-v2.13.2...oikontrol-v2.14.0) (2026-05-15)


### Features

* improve sequencer clip-start controls ([973865e](https://github.com/oikoaudio/bitwig-oikontrol/commit/973865e9cd3212c1c89bc6f51a56231cfc23f09d))
* show sequencer clip-start position on pads ([a17c503](https://github.com/oikoaudio/bitwig-oikontrol/commit/a17c5032df3f0c2c40694945f8c04a929732b7da))

## [2.13.2](https://github.com/oikoaudio/bitwig-oikontrol/compare/oikontrol-v2.13.1...oikontrol-v2.13.2) (2026-05-15)


### Bug Fixes

* clarify Akai Fire encoder guide ([006af47](https://github.com/oikoaudio/bitwig-oikontrol/commit/006af47010c3f189ff6341b7474e5f7b1ef45fdb))

## [2.13.1](https://github.com/oikoaudio/bitwig-oikontrol/compare/oikontrol-v2.13.0...oikontrol-v2.13.1) (2026-05-15)


### Bug Fixes

* **fire:** improve Akai Fire encoder and mode control reliability ([#42](https://github.com/oikoaudio/bitwig-oikontrol/issues/42)) ([cfd001f](https://github.com/oikoaudio/bitwig-oikontrol/commit/cfd001fa05dc226c62e6e3bca410afff31c87da7))

## [2.13.0](https://github.com/oikoaudio/bitwig-oikontrol/compare/oikontrol-v2.12.0...oikontrol-v2.13.0) (2026-05-14)


### Features

* **fire:** improve nested rhythm generation ([#38](https://github.com/oikoaudio/bitwig-oikontrol/issues/38)) ([3a99e1a](https://github.com/oikoaudio/bitwig-oikontrol/commit/3a99e1a0b1e71c939d2972ee5e77a1d5f3877ea6))
* rework nested rhythm ranking and density controls ([#40](https://github.com/oikoaudio/bitwig-oikontrol/issues/40)) ([793d3bf](https://github.com/oikoaudio/bitwig-oikontrol/commit/793d3bfc532793c2266312faed3affcecc6e07ce))

## [2.12.0](https://github.com/oikoaudio/bitwig-oikontrol/compare/oikontrol-v2.11.0...oikontrol-v2.12.0) (2026-05-02)


### Features

* add nested rhythm clustering controls ([#36](https://github.com/oikoaudio/bitwig-oikontrol/issues/36)) ([d4d0572](https://github.com/oikoaudio/bitwig-oikontrol/commit/d4d0572fac9cef17303ee661813cdba45fa77f32))

## [2.11.0](https://github.com/oikoaudio/bitwig-oikontrol/compare/oikontrol-v2.10.0...oikontrol-v2.11.0) (2026-05-02)


### Features

* **fire:** add Akai Fire DRUM Grid64, Velocity, and Bongos layouts ([#35](https://github.com/oikoaudio/bitwig-oikontrol/issues/35)) ([7481a8f](https://github.com/oikoaudio/bitwig-oikontrol/commit/7481a8f3f16509bc03c54b76a81ab60a5c180ab4))


### Bug Fixes

* **fire:** track selection got stuck on hidden tracks ([#33](https://github.com/oikoaudio/bitwig-oikontrol/issues/33)) ([93fcaa1](https://github.com/oikoaudio/bitwig-oikontrol/commit/93fcaa1fe3164d38c853ac243ba0b0e00a030025))

## [2.10.0](https://github.com/oikoaudio/bitwig-oikontrol/compare/oikontrol-v2.9.0...oikontrol-v2.10.0) (2026-04-26)


### Features

* add controller navigation and clip recording controls ([#31](https://github.com/oikoaudio/bitwig-oikontrol/issues/31)) ([b15ffc6](https://github.com/oikoaudio/bitwig-oikontrol/commit/b15ffc6f19c39195d537c0c7fb5ef4495a8f888e))

## 2.9.0 - Perform scene launch page and Rec+pad recordin
- New Perform scene launch page (toggle on Perform)
- REC + pad recording
- ALT + PERFORM toggles horizontal/vertical clip launch pad view
- Renamed the controller display name (Fire Oikontrol) to fit in better with other controllers
- Distribute a single bwextension built from the two modules. If you want the individual controller extension those can still be built from source using the just-commands

## 2.8.0 - Fugue mode and generative workflow polish
- Added Akai Fire `Fugue` step mode. It uses MIDI channel 1 as a template melodic line and generates derived new lines to channels 2-4 in the same clip, using Bach-type operations (transpose/reverse/move etc).
- Added `REC + pad` in Akai Fire `PERFORM` mode to target recording directly into a visible launcher slot using the configured default clip length.
- Moved Akai Fire `PERFORM` vertical/horizontal launcher layout switching to `ALT + PERFORM`.
- Added a latched Akai Fire `PERFORM` `Scene Launch` pad page with top-row scene launch, select, copy, and delete actions.
- Swapped Nested Rhythm pattern gestures so `PATTERN DOWN` generates the current nested rhythm and `PATTERN UP` resets hit edits, to better match related gestures in other modes.
- Updated the user guide and bundled help with notes on the new Fugue workflow.

## 2.7.0 - Nested Rhythm mode and global control fixes
- Added a new Akai Fire `Nested Rhythm` drum mode for generating rhythm from nested segment divisions rather than fixed-grid step placement. The mode slices segments into symmetric and asymmetric subdivisions (tuplets and ratches) in a musically interesting way, and adds expression layer profiles for pressure, timbre, velocity, and chance that can be rotated across the generated hits. Rhythmic structures can be changed on the fly, generated hits can be embellished with per-hit edits, and recurrence can be generated or edited using the same gesture as in `Melodic Step`.
- (improvement) `PERFORM` now exits the `SHIFT + PERFORM` track-action page for easier exit
- `SHIFT + BROWSER` global-settings access is more robust, including in Drum Seq
- global root/scale/octave settings now use slower stepped encoders, clamped scale bounds, and safer scale lookup handling

## 2.6.0 - Perform track actions and encoder behavior refinement
- Added a `SHIFT + PERFORM` track-action page for `Stop`, `Solo`, `Mute`, and `Arm`
- Unified Akai Fire encoder turn and touch-reset behavior behind shared control helpers
- Added shared encoder value profiles for parameter-style controls and aligned more encoder paths with the shared behavior layer
- Softened live and step expression encoder feel for pressure, timbre, and related expression controls
- Reduced encoder touch-reset hold from `1000 ms` to `750 ms`
- Aligned live note timbre reset/default with zero-based semantics instead of centering on `64`
- Renamed active chord-step bank/state terminology from `Oikord` to `Chord`

## 2.5.0 - Harmonic note input, step routing, and control-surface polish
- Added a harmonic live `NOTE` submode with harmonic lattice layout, multi-note pad output, note-count selection, octave-span control, bass-column/full-field variants, and harmonic gliss support
- Added live-note pitch-bend with spring-return, and toggle between `5th/8v` and `ScaleDeg` gliss modes
- Moved `Chord Step` behind `STEP SEQ` so `NOTE` stays focused on live note input; pressing `STEP SEQ` now enters `Melodic Step`, then switches to `Chord Step` on the next press
- Added `ALT + NOTE` and `SHIFT + Encoder 4` as direct live-layout shortcuts for collapsing note input to scale notes or switching harmonic layout variants
- Added horizontal `Perform` orientation plus broader encoder-scaling cleanup, melodic step expression-page updates, and Bitwig 6-aligned scale naming/order
- Reworked Fire modifier routing so `SHIFT + PERFORM` latches a 4-row track-action page (`Stop`, `Solo`, `Mute`, `Arm`) and `SHIFT + BROWSER` opens a global root/scale/octave settings overlay
- Updated tests and Fire documentation to match the new harmonic input, step-routing, and live-control workflows

## 2.4.0 - Akai Fire shared pitch context and architecture rework
- Added a unified shared musical context for `Root Key`, `Scale`, and `Octave` across live `NOTE`, `Chord Step`, `Melodic Step`, and `SHIFT + PERFORM`
- Added melodic recurrence editing with held-step targeting, top-row recurrence gestures, recurrence feedback, and supporting tests
- Reworked Fire mode architecture by separating live note play from chord sequencing, extracting dedicated controllers/helpers, and simplifying duplicated framework code
- Expanded `SHIFT + PERFORM` overview editing for shared pitch controls and refreshed note/chord workflows to use the shared context consistently
- Updated `Chord Step` interpretation and melodic generator workflows, plus docs and bundled help, to reflect the new shared-pitch model

## 2.3.1 - Akai Fire selected clip sync fix
- Fixed bidirectional sync of selected clip state so shared clip selection follows the actual Bitwig `isSelected()` state instead of the controller cursor index
- Updated chord and melodic step refresh paths to trust an already selected clip before falling back to the playing clip
- Prevented passive refresh from needlessly reselecting clip slots

## 2.3.0 - Akai Fire shared sequencer clip row and step-workflow polish
- Added a shared sequencer clip row across Drum, Chord Step, and Melodic Step workflows
- Polished `Melodic Step` generation and editing behavior, including safer held-step and accent interactions
- Added reusable accent-latch and clip-row helper logic to stabilize sequencer behavior across modes
- Fixed browser-selection behavior and refreshed documentation for the updated Fire sequencing workflow

## 2.2.0 - Akai Fire shared pitch and safer performance workflows
- Added live NOTE `Pitch Gliss` with held-note retuning and a dedicated velocity response model based on `Velocity Sensitivity` and `Default Velocity`
- Added shared global `Root Key` and `Scale` across live NOTE, Chord Step, and the latched `SHIFT + PERFORM` `Settings` page
- Added Fire startup preferences for `Default Root Key` and `Default Velocity Sensitivity`, and narrowed the default note-input octave choices to `2`, `3`, and `4`
- Applied the same velocity-sensitivity model to Chord Step and made its builder default to in-key with visible-only auto-seeding
- Added clearer safety checks in Chord Step and Melodic Step for missing clips, wrong track types, hanging live notes, and explicit clip clearing
- Added relative clip halve/double gestures on `ALT + BANK LEFT/RIGHT` across Drum, Chord Step, and Melodic Step workflows
- Added a latched Fire `Settings` mode with dedicated OLED/button feedback and refreshed Perform encoder-page labeling
- Updated tests and Fire documentation to match the new shared pitch, settings, and sequencing workflow

## 2.1.0 - Akai Fire melodic generation and workflow pass
- Added `Melodic Step` mode under `STEP` with phrase engines for `Acid`, `Motif`, `Call/Resp`, `Euclid`, `Rolling`, and `Octave`
- Added editable melodic pitch-pool generation, mutation, note audition, and cleaner preservation of manual pool edits
- Reworked Fire encoder banks and step-sequencer controls to align behavior across drum, note, chord, and melodic workflows
- Added step paging plus fine-nudging for chord sequencing, while leaving unreliable rotation behavior disabled for now
- Added default note-input octave control, stronger OLED and pad feedback, and generator family feedback during melodic generation
- Updated docs, tests, and Fire workflow preferences to match the 2.1 sequencing pass

## 2.0.0 - Akai Fire and multi-controller repo
Adds a full Akai Fire extension and promotes `bitwig-oikontrol` into a multi-controller repository:
- Added a full Akai Fire extension under `modules/akai-fire`
- Added Fire `DRUM`, `NOTE`, and `PERFORM` workflows
- Added drum step sequencing with fine nudge, accent/fill handling, and Euclid controls
- Added note mode with isomorphic layout, scale/root/octave controls, and note-step access
- Added chord-step sequencing and performance clip launching
- Added Fire OLED feedback, encoder pages, transport/tempo control, and preferences
- Restructured the repository into a multi-module `bitwig-oikontrol` project for Akai Fire and Launch Control XL
- Continued Launch Control XL improvements, including device remote pages and drum-mode refinements
- Updated docs, tests, and build tooling

## 1.0.0 — Novation Launch Control XL Oikontrol script
Mirrors the extension for the Novation Launch Control XL mk2 that ships with Bitwig, with the following additions:
- Dedicated User Template 8 incorporates Eric Ahrens' and Richie Hawtin's ARP as implemented in the rhbitwig extension
- User templates 1–7 pass raw MIDI into Bitwig so they can be mapped to plugins using MIDI CC mapping,
  or to Bitwig targets using the project MIDI mapping functionality
- LEDs indicate factory mode knob values (off, low, high)
