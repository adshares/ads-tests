@vip_key
Feature: Vip key features

  Scenario: Check vip keys
    Given user, who wants to check vip keys
    When user checks vip keys
    Then all vip keys are correct

  Scenario: Change vip keys
    Given vip user, who wants to change own node key
    And user, who wants to check vip keys
    And user checks vip keys
    When user changes own node key
    And after delay user checks vip keys
    Then vip keys are changed
    And all vip keys are correct

  Scenario: Check log
    Given user log

  Scenario: Check transaction
    Given transaction ids
