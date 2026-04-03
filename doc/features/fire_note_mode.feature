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

  Scenario: First pass uses local scale state
    Given Note mode is active
    Then the Fire uses its local root-note and scale settings
    And host-scale follow is explicitly deferred until the API exposes dependable global scale state

  Scenario: Note mode keeps setup controls on the hardware
    Given Note mode is active
    When I use the assigned setup controls
    Then I can change octave from the hardware
    And I can switch between chromatic and scale-aware behavior
    And I can see which note mode options are currently active

  Scenario: Step Seq is reserved while note step-entry is deferred
    Given Note mode is active
    When I press STEP SEQ
    Then the controller toggles between chromatic and in-key note layout
    And note step-entry remains explicitly deferred in this first pass

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
    And the current role of STEP SEQ inside Note mode is documented clearly
    And the main encoder can use the last touched Bitwig parameter when that preference is enabled
    And the OLED shows note-mode setup values and last-touched parameter feedback where available
    And host-scale follow is documented as deferred until the API exposes the required state
    And the README and change notes describe Note mode and its limitations

  Scenario: Testing
    When the feature is verified
    Then automated tests cover any extracted note-mapping logic for chromatic and scale-aware layouts
    And manual testing confirms notes are transmitted correctly in Drum mode versus Note mode
    And manual testing confirms octave and scale changes update the played notes as shown on the display
    And manual testing confirms the main encoder can adjust the last touched Bitwig parameter when the preference is enabled
    And manual testing confirms returning from Note mode to Drum mode restores the sequencer bindings cleanly
