Feature: Akai Fire Euclid simplification
  As the primary user of the Akai Fire script
  I want Euclid generation to be fast and predictable
  So it is genuinely quicker than entering the pattern by hand.

  Background:
    Given the Akai Fire is connected
    And the controller is running the Oikontrol extension
    And Drum mode is active

  Scenario: Euclid focuses on the useful controls
    When I switch the encoder bank to the Euclid controls
    Then the primary Euclid controls expose pattern length and pulses
    And rotate is no longer a required Euclid parameter
    And invert is no longer a required Euclid parameter
    And grid-button pattern shifting remains available for moving the result in time

  Scenario: Euclid applies immediately when its values change
    Given a drum lane is selected
    When I turn the Euclid length or density encoder
    Then the Euclidean pattern is regenerated for the selected lane immediately
    And the display shows the current Euclid values
    And the Browser button is no longer required to commit the pattern

  Scenario: Euclid rewrites the lane only when necessary
    Given a drum lane is selected
    And a Euclidean pattern is already applied for the current length and pulse values
    When the encoder input does not change the effective Euclid values
    Then the lane is not cleared and rewritten again
    When the effective Euclid values change
    Then the display shows the current pulse count
    And the target lane is cleared and rewritten exactly once for that change

  Scenario: Euclid pulse selection is less sensitive
    When I make small movements on the Euclid density encoder
    Then the pulse count changes in a controlled, less fiddly way
    And it is practical to stop on the intended pulse count without overshooting

  Scenario: Euclid application is deterministic
    Given the selected lane already contains notes
    When I change the Euclid length or pulse value
    Then the target lane is rewritten according to the documented behavior
    And the implementation does not expose unused or misleading apply flags

  Scenario: Definition of done
    Then Euclid can be operated using length and pulses without relying on Browser for apply
    And rotate and invert are removed or clearly deferred from the active control surface
    And Euclid updates the clip contents immediately when length or pulse values change
    And pulse selection feels less sensitive than the current implementation
    And the on-screen feedback reflects the simplified controls
    And the README and change notes describe the updated Euclid workflow

  Scenario: Testing
    When the feature is verified
    Then automated tests cover Euclidean pattern generation for representative length and pulse combinations
    And automated tests cover any pulse-detent or sensitivity helper logic extracted from the encoder handler
    And automated tests cover the rewrite guard that skips no-op updates
    And manual testing confirms encoder changes update the written pattern immediately
    And manual testing confirms the written pattern matches the shown length and pulse count
    And manual testing confirms pattern shifting on grid buttons still provides the practical replacement for rotate
