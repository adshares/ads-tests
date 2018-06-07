@status
Feature: Account features

  Scenario Outline: Set status <type_a> -> <type_b>
    Given <type_a> account user, who wants to change <type_b> status
    When user changes status
    Then only <available_bits> could be changed
    Examples:
      |type_a |type_b|available_bits|
      |main   |own   |2-16          |
      |main   |local |2-16          |
      |main   |remote|              |
      |regular|own   |2-16          |
      |regular|local |2-4           |
      |regular|remote|              |

  Scenario: Check log
    Given user log