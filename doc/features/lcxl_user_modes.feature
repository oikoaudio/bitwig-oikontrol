Feature: Launch Control XL single-device layer
  As the primary user of the LCXL script
  I want a user template that dedicates all controls to the selected device
  So I can treat the LCXL as a DFAM-style remote-page surface.

  Background:
    Given the Launch Control XL is connected
    And the controller is running the Oikontrol extension

  Scenario: Device-focused layer spreads remote pages across hardware
    When I select the user template assigned to the device-focused layer
    And the cursor track has a selected device
    Then the layer becomes active
    And knob row 1 controls device remote page 1
    And knob row 2 controls device remote page 2
    And knob row 3 controls device remote page 3
    And faders control device remote page 4
    And the track focus buttons control device remote page 5
    And the track control buttons control device remote page 6
    And transport-side buttons remain available for mode toggles and navigation as designed
