Feature: Akai Fire note mode
  As the primary user of the Akai Fire script
  I want a dedicated note-input mode on the Fire
  So I can play chromatic or in-scale notes without leaving the controller.

  Background:
    Given the Akai Fire is connected
    And the controller is running the Oikontrol extension
    And the extension targets Bitwig controller API 25

  Scenario: Note button enters a keyboard-oriented mode
    When I press the NOTE button
    Then Note mode becomes active
    And the pad grid is repurposed for note entry instead of drum sequencing
    And the DRUM button returns the controller to Drum mode
    And the PERFORM button can switch from Note mode into Perform mode

  Scenario: Note mode supports chromatic and scale-aware playing
    Given Note mode is active
    When I inspect the active note layout
    Then I can use a chromatic layout
    And I can use a scale-aware layout
    And the current root note and octave are visible on the display when changed

  Scenario: Note mode can follow Bitwig 6 scale when supported
    Given Bitwig 6 global scale information is available through the API in use
    When follow-host-scale is enabled
    Then the Fire note layout follows the Bitwig root note and scale
    When follow-host-scale is unavailable or disabled
    Then the Fire falls back to its local root-note and scale settings

  Scenario: Note mode keeps setup controls on the hardware
    Given Note mode is active
    When I use the assigned setup controls
    Then I can change octave from the hardware
    And I can switch between chromatic and scale-aware behavior
    And I can see which note mode options are currently active

  Scenario: Step Seq can become note step-entry control in Note mode
    Given Note mode is active
    When I press STEP SEQ
    Then the controller can enter or advance a note step-entry workflow as designed for Note mode
    When I hold SHIFT and press STEP SEQ
    Then the alternate note step-entry action is available if this workflow is included
    And the exact step-entry interaction is documented as a Note-mode-specific behavior

  Scenario: Main encoder can target the last touched parameter
    Given the main encoder role preference is set to last touched
    When I turn the main encoder in Note mode
    Then the last touched Bitwig parameter is adjusted
    And Note mode continues to receive note input on the pad matrix
    And the OLED shows the touched parameter name or value when it changes

  Scenario: Definition of done
    Then NOTE reliably enters Note mode and DRUM reliably returns to Drum mode
    And PERFORM can switch from Note mode into Perform mode
    And Note mode supports at least chromatic and one scale-aware layout
    And root note, octave, and scale selection are controllable and visible
    And the role of STEP SEQ inside Note mode is documented if step-entry behavior is included
    And the main encoder can use the last touched Bitwig parameter when that preference is enabled
    And the OLED shows note-mode setup values and last-touched parameter feedback where available
    And host-scale follow is implemented if API 25 exposes the required state, otherwise the fallback is documented clearly
    And the README and change notes describe Note mode and its limitations

  Scenario: Testing
    When the feature is verified
    Then automated tests cover any extracted note-mapping logic for chromatic and scale-aware layouts
    And automated tests cover host-scale fallback behavior when API state is absent
    And manual testing confirms notes are transmitted correctly in Drum mode versus Note mode
    And manual testing confirms octave and scale changes update the played notes as shown on the display
    And manual testing confirms the main encoder can adjust the last touched Bitwig parameter when the preference is enabled
    And manual testing confirms returning from Note mode to Drum mode restores the sequencer bindings cleanly
