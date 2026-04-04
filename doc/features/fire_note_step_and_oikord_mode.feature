Feature: Akai Fire note step modes and Oikord sequencing
  As the primary user of the Akai Fire script
  I want NOTE and ALT plus NOTE to own the note-family navigation grammar
  So I can switch cleanly between live note input and harmonic step input without colliding with Drum controls.

  # Planning note:
  # This document is intentionally a design and handoff spec for the next pass.
  # It recommends a UI partition rather than treating every idea as one merged mode.

  Background:
    Given the Akai Fire is connected
    And the controller is running the Oikontrol extension
    And NOTE mode already supports live note input with chromatic and in-key layouts
    And Drum mode already uses STEP SEQ for Accent and SHIFT plus STEP SEQ for Fill

  Scenario: NOTE switches between live note input and Oikord Step
    Given NOTE mode is active
    Then NOTE mode itself means live note play
    And the pad grid plays notes immediately in the selected note layout
    When I press NOTE in live Note mode
    Then NOTE mode enters Oikord Step
    And the pad grid is repurposed for note editing instead of direct note play
    When I press NOTE again from Oikord Step
    Then NOTE mode returns to live note play
    And NOTE mode is not treated as a third peer step sub-mode alongside step-entry states

  Scenario: Oikord Step is the first dedicated harmonic sequencer mode
    Given NOTE mode is active
    When I press NOTE to enter Oikord Step
    Then the Fire enters Oikord Step mode
    And the lower two pad rows represent 32 visible note steps
    And the upper two pad rows represent 32 visible concrete Oikord variants
    And Pattern Up and Pattern Down switch between the two visible Oikord pages
    And the total visible Oikord pool is at most 64 curated voicings across two pages
    And the Oikord pads represent concrete assignable harmonic objects rather than abstract family placeholders

  Scenario: Oikord Step assigns harmonic changes by combining a held step and an Oikord slot
    Given Oikord Step mode is active
    When I hold one or more steps on the lower two rows
    And I press an Oikord slot on the upper two rows
    Then the selected Oikord is assigned to those held steps
    And the controller writes literal notes into the clip for those steps
    And the first implementation does not depend on hidden controller-only symbolic playback state
    And the assigned steps show clear LED feedback for occupied versus empty state
    And the OLED shows the selected Oikord name or short label in a legible form

  Scenario: Oikord Step uses pads as the primary assignment gesture and encoders as secondary editors
    Given Oikord Step mode is active
    And a step is selected
    Then the primary interaction is hold step and tap Oikord pad
    When I turn the small encoders
    Then I can perform secondary edits such as family browsing, variant browsing, voicing register offset, or gate-related edits
    And the encoders are not the only way to choose a harmonic object
    And the final encoder mapping is documented explicitly
    And the mapping prioritizes fast browsing over parameter overload

  Scenario: The first Oikord implementation is curated rather than exhaustive
    Given the Oikords reference material contains many packs and families
    Then the first Fire Oikord implementation uses a curated best-of subset
    And the initial subset contains at most eight harmonic families
    And each family exposes a small, hardware-browsable set of variants
    And the selected families are chosen for modal usefulness, sparse voicing quality, and clear differentiation
    And the curated family order starts with Barker and then Audible before the more overtly modal color families
    And the curation favors a shortlist such as Barker, Audible, Sus Motion, Quartal, Cluster, Minor Drift, Dorian Lift, and Rootless or Pedal
    And the final shortlist is documented together with its source packs

  Scenario: Barker is the first family and maps to the classic source set
    Given the first Oikord implementation is being curated
    Then the first visible family is named Barker on the Fire surface
    And Barker is sourced from the classic reference material rather than exposing the raw classic pack name
    And Barker acts as the primary familiar harmonic anchor for the rest of the Oikord bank

  Scenario: Audible keeps its own voicing identity
    Given the Audible family is included in the first Oikord implementation
    Then Audible is sourced from the Plaits-derived reference material
    And Audible uses its dedicated Plaits-style voicing rotation rather than the generic voicing path
    And the implementation preserves the recognisable identity of the original module behavior

  Scenario: Default rendering choices are fixed for the first pass
    Given the Oikords reference code supports multiple traversal and register choices
    Then the first Fire Oikord implementation uses the controlled-envelope traversal that keeps bass and top contours developing predictably
    And the default register is MID
    And those rendering choices are fixed in the first pass rather than exposed as hardware parameters
    And the exception is that Audible still uses its own dedicated voicing rotation behavior

  Scenario: Oikord Step works as harmonic sequencing rather than pop-chord memory
    Given Oikord Step mode is active
    Then the available Oikords are treated as modal voicing rotations or sparse harmonic colors
    And they are usable as additive or spectral source material rather than only as dense keyboard chords
    And transposition and scale context from NOTE mode continue to affect how those voicings are heard
    And the surface language avoids presenting them as conventional songwriter chord presets

  Scenario: ALT plus NOTE switches the two-state variant of the active note-family surface
    Given NOTE mode is active
    When I hold ALT and press NOTE in live Note mode
    Then the note layout toggles between chromatic and in-key
    When I press NOTE to enter Oikord Step
    And I hold ALT and press NOTE
    Then the Oikord interpretation toggles between As Is and Cast

  Scenario: STEP SEQ stays Drum-owned while NOTE owns note-family navigation
    Given NOTE mode is active
    Then NOTE owns switching between live Note and Oikord Step
    And ALT plus NOTE owns switching the current note-surface variant
    And SHIFT plus NOTE remains available for SNAP or another labeled utility rather than carrying Oikord interpretation
    And STEP SEQ is no longer required for Oikord Step access
    And this partition avoids making Drum Accent and Fill compete with note-family entry

  Scenario: Recommended UI partition for the next implementation pass
    Then the preferred NOTE partition is:
      | Surface             | Plain NOTE                               | ALT plus NOTE                            |
      | Live Note           | switch to Oikord Step                    | toggle chromatic versus in-key           |
      | Oikord Step         | return to Live Note                      | toggle As Is versus Cast                 |
    And NOTE keeps one consistent cross-surface meaning
    And ALT plus NOTE keeps one consistent within-surface meaning

  Scenario: Definition of done
    Then NOTE mode still supports regular note play as its default meaning
    And NOTE can enter a dedicated Oikord Step mode without breaking the existing NOTE controls
    And Oikord Step can assign curated harmonic states to 32 visible steps using direct hardware gestures
    And the first Oikord pass writes literal notes into the clip instead of maintaining a hidden symbolic playback layer
    And Clip Step Record is either explicitly deferred or reintroduced later with a clean third-surface navigation rule
    And the curated Oikord subset and its source packs are documented clearly
    And OLED and LED feedback remain legible and mode-specific
    And the README and controller-layout documentation describe the final NOTE and STEP partition

  Scenario: Testing
    When the feature is verified
    Then automated tests cover any extracted Oikord-bank paging or step-assignment logic
    And automated tests cover any extracted curated-family selection mapping
    And manual testing confirms Live Note Play remains intact when returning from Oikord Step
    And manual testing confirms Oikord assignment gestures do not leave stuck notes or corrupt clip data
    And manual testing confirms NOTE reliably switches between Live Note and Oikord Step
    And manual testing confirms ALT plus NOTE reliably toggles the correct two-state variant in each surface
