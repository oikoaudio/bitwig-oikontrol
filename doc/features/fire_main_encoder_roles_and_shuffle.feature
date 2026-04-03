Feature: Akai Fire main encoder roles and shuffle control
  As the primary user of the Akai Fire script
  I want the main SELECT encoder to switch cleanly between a few persistent roles
  So I can use one central control for parameter editing, shuffle, or note repeat without diving into settings every time.

  Background:
    Given the Akai Fire is connected
    And the controller is running the Oikontrol extension
    And the extension targets Bitwig controller API 24

  Scenario: Main encoder roles remain preference-backed
    Given the extension preferences are open
    When I inspect the main encoder options
    Then I can choose from at least these roles:
      | role |
      | Last Touched Parameter |
      | Shuffle |
      | Note Repeat |
      | Disabled |
    And the selected role is persisted as an extension preference

  Scenario: Shift plus encoder press cycles the persistent role
    Given Drum mode is active
    When I hold SHIFT and press the main SELECT encoder
    Then the main encoder role advances to the next configured role
    And the new role is stored in the same preference used by the settings UI
    And the OLED shows the new role name immediately

  Scenario: Cycling roles does not steal the unmodified encoder press
    Given Drum mode is active
    When I press the main SELECT encoder without SHIFT
    Then the press action remains available to the currently active role
    And unmodified press is not used to cycle the encoder role

  Scenario: Last touched parameter role adjusts a Bitwig parameter
    Given the main encoder role is Last Touched Parameter
    When I turn the main SELECT encoder
    Then the current parameter target is adjusted
    And the OLED shows the parameter name and current value if available

  Scenario: Shuffle role controls groove amount and enabled state
    Given the main encoder role is Shuffle
    When I turn the main SELECT encoder
    Then Bitwig's global groove shuffle amount is adjusted
    And the OLED shows Shuffle and the current amount
    When I press the main SELECT encoder
    Then Bitwig groove is toggled on or off
    And the OLED shows whether Shuffle is on or off

  Scenario: Note repeat role keeps its dedicated behavior
    Given the main encoder role is Note Repeat
    When I turn the main SELECT encoder
    Then the note repeat rate changes
    And the OLED shows the current note repeat value
    When I press the main SELECT encoder
    Then note repeat is toggled on or off

  Scenario: Disabled role leaves the encoder inactive unless another gesture owns it
    Given the main encoder role is Disabled
    When I turn the main SELECT encoder
    Then no normal-role action is triggered
    And the OLED does not imply that a value changed
    When I press the main SELECT encoder
    Then no normal-role action is triggered

  Scenario: Role-specific gestures still yield to higher-priority temporary gestures
    Given Drum mode is active
    When I hold Accent and use the main SELECT encoder
    Then Accent velocity editing takes priority over the persistent main encoder role
    And SHIFT plus press remains reserved for encoder-role cycling

  Scenario: Definition of done
    Then the main encoder role can be changed either in settings or with SHIFT plus encoder press
    And the available roles include Last Touched Parameter, Shuffle, Note Repeat, and Disabled
    And role changes persist across controller restarts
    And the OLED reports the current encoder role when cycling it
    And turning the encoder shows a useful current value for the active role whenever the API exposes one
    And shuffle control uses Bitwig's groove shuffle amount and groove enabled state
    And note repeat behavior remains available when that role is selected
    And accent editing and other higher-priority temporary gestures are not broken by the role system

  Scenario: Testing
    When the feature is verified
    Then automated tests cover main-encoder role cycling and preference persistence logic
    And automated tests cover any extracted mapping from role names to runtime actions
    And manual testing confirms SHIFT plus encoder press cycles roles and updates the OLED
    And manual testing confirms Shuffle role turns groove on and off and adjusts the amount shown on the OLED
    And manual testing confirms Note Repeat role still toggles and edits repeat values correctly
    And manual testing confirms Last Touched Parameter role still adjusts its target and shows feedback
