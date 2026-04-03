Feature: Akai Fire drum layout and preferences cleanup
  As the primary user of the Akai Fire script
  I want the drum sequencer layout and button roles to match the hardware more closely
  So the default mode is easier to learn and leaves dedicated buttons free for future modes.

  Background:
    Given the Akai Fire is connected
    And the controller is running the Oikontrol extension
    And the extension targets Bitwig controller API 25

  Scenario: Drum mode uses a fixed four-row layout
    When I enter Drum mode
    Then the top row of pads controls clip slots
    And the second row of pads controls visible drum slots
    And the bottom two rows control the 32 visible steps of the selected drum lane
    And the "Second Row" extension preference is no longer shown

  Scenario: Clip launch behavior is configured in preferences
    Given the extension preferences are open
    When I inspect the clip launch options
    Then I can choose whether non-default clip launches are synced or from-start
    And I can choose the clip launch quantization amount
    And the NOTE button is no longer used to toggle clip launch quantization at runtime

  Scenario: Drum and Note buttons become mode selectors
    When I press the DRUM button
    Then Drum mode becomes active
    And the DRUM button reflects the active mode
    When I press the NOTE button
    Then Note mode becomes active
    And the NOTE button reflects the active mode

  Scenario: Perform becomes a third top-level mode
    When I press the PERFORM button
    Then Perform mode becomes active
    And the PERFORM button reflects the active mode
    And PERFORM is no longer reserved only for temporary grid-resolution editing

  Scenario: Drum button can advance through drum sub-modes
    Given Drum mode is already active
    When I press the DRUM button again
    Then the controller advances to the next available drum sub-mode
    And the DRUM button color indicates which drum sub-mode is active
    And the sub-mode cycle leaves NOTE and PERFORM as separate top-level modes

  Scenario: Step Seq becomes the primary accent gesture
    When I hold STEP SEQ in Drum mode and press an empty step
    Then a new accented note is created on that step
    When I hold STEP SEQ in Drum mode and press an existing normal note
    Then that note becomes accented
    When I hold STEP SEQ in Drum mode and press an existing accented note
    Then that note returns to the normal velocity
    And the dedicated DRUM button remains reserved for mode selection

  Scenario: Fill moves to Shift plus Step Seq
    When I hold SHIFT and press STEP SEQ in Drum mode
    Then Bitwig Fill toggles while the command is held or activated as designed

  Scenario: Grid-resolution editing moves to Alt plus grid arrows
    When I hold ALT and press BANK LEFT or BANK RIGHT in Drum mode
    Then the grid resolution is adjusted down or up
    And SHIFT plus the grid arrows remains available for fine pattern nudging

  Scenario: Pattern button behavior is configurable and metronome remains fixed on shift
    Given the extension preferences are open
    When I inspect the Pattern button options
    Then I can choose the primary PATTERN button action from a small list of supported utilities
    And the default PATTERN action is Clip Launcher Automation Write
    And SHIFT plus PATTERN toggles the metronome

  Scenario: Main encoder role is configurable and uses the OLED
    Given the extension preferences are open
    When I inspect the main encoder options
    Then I can choose whether the main encoder controls the last touched parameter or note repeat
    And OLED feedback shows the active parameter name or value whenever the encoder changes something

  Scenario: Main encoder can operate note repeat when configured
    Given the main encoder role preference is set to note repeat
    When I press the main encoder once
    Then note repeat is enabled
    When I turn the main encoder
    Then the note repeat value changes
    And the OLED shows the current note repeat value
    When I press the main encoder again
    Then note repeat is disabled

  Scenario: Pinning is handled by settings before claiming a prime button
    Given the extension preferences are open
    When I inspect the pinning options
    Then I can choose whether automatic pinning is enabled for the relevant modes
    And pinning does not require a dedicated prime front-panel button in the first iteration

  Scenario: Definition of done
    Then the Fire no longer relies on the "Second Row" preference to choose between clips and pad functions
    And clip launch quantization is controlled only by extension preferences
    And DRUM, NOTE, and PERFORM are available for top-level mode selection
    And DRUM can indicate the active drum sub-mode by color if multiple drum sub-modes are implemented
    And holding STEP SEQ provides accent editing and accented-note entry in Drum mode
    And SHIFT plus STEP SEQ triggers Fill
    And PATTERN is configurable while SHIFT plus PATTERN toggles the metronome
    And the main encoder role is configurable between at least last touched and note repeat
    And ALT plus the grid arrows changes grid resolution without conflicting with shift and nudge actions
    And the default drum layout is clips on top, drum slots on the second row, and steps on the bottom two rows
    And the OLED is used to show parameter names or values whenever practical
    And the README and change notes describe the new layout and preference model

  Scenario: Testing
    When the feature is verified
    Then automated tests cover any extracted pure mapping or preference-selection logic
    And manual testing confirms clip launch, drum-slot selection, and 32-step entry on hardware
    And manual testing confirms preference changes alter clip launch behavior without requiring remapped runtime buttons
    And manual testing confirms holding STEP SEQ can create accented notes and toggle accent on existing notes
    And manual testing confirms SHIFT plus STEP SEQ triggers Fill without breaking Accent editing
    And manual testing confirms PATTERN runs the configured utility while SHIFT plus PATTERN toggles metronome
    And manual testing confirms ALT plus the grid arrows changes grid resolution while SHIFT plus the same arrows still performs fine nudging
    And manual testing confirms the OLED shows parameter names or values for the main encoder and other edited controls where available
