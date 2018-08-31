@transfer
@transfer_remote
Feature: Remote transfers

  Scenario: Send 100 ADS
    Given 2 users in different node
    When sender sends 100 ADS to receiver
    And wait for balance update
    Then receiver balance is as expected (changed by amount)
    And sender balance is as expected (changed by amount and fee)

  Scenario: Send minimum amount of ADS
    Given 2 users in different node
    When sender sends 0.00000000001 ADS to receiver
    And wait for balance update
    Then receiver balance is as expected (changed by amount)
    And sender balance is as expected (changed by amount and fee)

  Scenario Outline: Send non existing <amount> ADS
    Given 2 users in different node
    When sender sends <amount> ADS to receiver
    And wait for balance update
    Then receiver balance is as expected
    And sender balance is as expected
    Examples:
      | amount     |
      | 40000000   |
      | -1         |
      | -100000    |
      | 2147483648 |

  Scenario: Send many
    Given 3 users in different node
    When sender sends 100 ADS to receivers
    And wait for balance update
    Then receiver balance is as expected (changed by amount)
    And sender balance is as expected (changed by amount and fee)

  Scenario: Send many with summary amount exceeding balance (invalid)
    Given 3 users in same node
    When sender sends many transfers which sum exceeds balance
    Then transfer is rejected

  Scenario: Send transfer with message
    Given 2 users in different node
    When sender sends 0.00001000000 ADS to receiver with message
    And wait for balance update
    Then receiver can read message
    And receiver balance is as expected (changed by amount)
    And sender balance is as expected (changed by amount and fee)

  Scenario: Send all collected funds (no fee included)
    Given 2 users in different node
    When sender sends all to receiver (fee is not included)
    And wait for balance update
    Then receiver balance is as expected
    And sender balance is as expected

  Scenario: Send all collected funds (fee included)
    Given 2 users in different node
    When sender sends all to receiver (fee is included)
    And wait for balance update
    Then receiver balance is as expected (changed by amount)
    And sender balance is as expected (changed by amount and fee)

  Scenario: Check log
    Given user log

  Scenario: Check transaction
    Given transaction ids
