Feature: Akai Fire polyrhythm drum mode
  As the primary user of the Akai Fire script
  I want a second drum-sequencer mode with one lane per row
  So I can build polyrhythms with independent lane lengths and per-lane generation tools.

  Background:
    Given the Akai Fire is connected
    And the controller is running the Oikontrol extension
    And a Drum Machine is hosted on a group or track in a way that exposes multiple drum lanes

  Scenario: Polyrhythm mode is entered as a drum sub-mode
    Given the standard Drum mode is active
    When I press the DRUM button again
    Then the controller advances to the polyrhythm drum sub-mode
    And the DRUM button color changes to show that the active drum sub-mode is no longer the standard one

  Scenario: A dedicated drum mode presents one visible lane per row
    When I switch to the polyrhythm drum mode
    Then each visible row on the pad matrix represents a separate drum lane
    And each row can target a different drum sound
    And row selection and row feedback make the active lane clear

  Scenario: Each row can have an independent loop length
    Given polyrhythm drum mode is active
    When I edit the length for one row
    Then that row length changes without forcing the same length on the other visible rows
    And playback reflects the independent row loop lengths

  Scenario: Each row can have an independent playback division
    Given polyrhythm drum mode is active
    When I edit the resolution or playback division for one row
    Then that row can run at a different rhythmic division from another row
    And the resulting playback supports polyrhythmic patterns

  Scenario: Per-row mute is the primary row action
    Given polyrhythm drum mode is active
    When I hold the left-side Mute button and press a visible row target
    Then the corresponding row or lane toggles mute
    And repeating the gesture restores the row from mute
    When I hold SHIFT and the left-side Mute button and press a visible row target
    Then the corresponding row or lane toggles solo if solo support is included in this mode
    And solo remains optional rather than required for the first implementation

  Scenario: Euclidean generation operates per row
    Given polyrhythm drum mode is active
    When I target a row and apply Euclidean generation
    Then the generated pattern applies only to that row
    And row length is respected by the generator
    And the per-row workflow makes Euclidean generation more useful than in single-lane mode

  Scenario: Definition of done
    Then the new mode is selectable as a DRUM sub-mode without regressing the original Drum mode
    And at least four visible rows can be edited as independent rhythmic lanes
    And each visible row has its own length control
    And per-row mute works from the hardware
    And Euclidean generation can be applied to an individual row
    And the README and change notes describe the new mode, its constraints, and any assumptions about track or Drum Machine setup

  Scenario: Testing
    When the feature is verified
    Then automated tests cover any extracted row-state, length, and division mapping logic
    And manual testing confirms multiple rows can run with different lengths without corrupting one another
    And manual testing confirms row mute toggles the intended lane and can be undone from hardware
    And manual testing confirms Euclidean generation respects the selected row and row length
    And manual testing confirms switching between the original Drum mode and the polyrhythm mode leaves the controller in a consistent state
