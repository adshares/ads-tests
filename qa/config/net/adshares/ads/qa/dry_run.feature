@dry-run
Feature: Dry-run

  Scenario: Check if dry-run return same data
    Given user, who wants to check dry-run
    When user sends transfer with dry-run
    When user sends transfer without dry-run
    Then data with dry-run is the same as without
