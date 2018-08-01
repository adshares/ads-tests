@dry_run
Feature: Dry-run

  Scenario: Check if dry-run return same data
    Given user, who wants to check dry-run
    When user sends transfer with dry-run
    And user sends transfer without dry-run
    Then data with dry-run is the same as without

  Scenario: Send transfer using signature from different account
    Given main and regular user, who want to check dry-run
    When regular user sends to main with dry-run
    And main user sends transfer using tx data

  Scenario: Check log
    Given user log

  Scenario: Check transaction
    Given transaction ids