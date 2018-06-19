@status
Feature: Status features

  Scenario Outline: Set account status <type_a> -> <type_b>
    Given <type_a> account user, who wants to change <type_b> account status
    When user changes account status
    Then only <available_bits> could be changed
    Examples:
      | type_a  | type_b | available_bits |
      | main    | own    | 2-16           |
      | main    | local  | 2-16           |
      | main    | remote |                |
      | regular | own    | 2-16           |
      | regular | local  | 2-4            |
      | regular | remote |                |

  Scenario Outline: Set node status <type_a> -> <type_b>
    Given <type_a> account user, who wants to change <type_b> node status
    When user changes node status
    Then only <available_bits> could be changed
    Examples:
      | type_a  | type_b | available_bits |
      | vip     | own    | 4-24           |
      | vip     | remote | 17-24          |
      | main    | own    | 4-16           |
      | main    | remote |                |
      | regular | own    |                |
      | regular | remote |                |

  Scenario: Set out of range account status
    Given main account user, who wants to change own account status
    When user tries to set out of range status
    Then change status transaction is rejected

  Scenario: Set out of range node status
    Given main account user, who wants to change own node status
    When user tries to set out of range node status
    Then change status transaction is rejected

  Scenario: Check log
    Given user log