@fee_sharing
Feature: Fee sharing

  Scenario: Separated
    Given top group is separated from vip group
    When collect all logs
    Then profit shared is as expected

  Scenario: Common
    Given top group has common part with vip group
    When collect all logs
    Then profit shared is as expected

  Scenario: Include all
    Given top group includes all from vip group
    When collect all logs
    Then profit shared is as expected

  Scenario: Check log
    Given user log

  Scenario: Check transaction
    Given transaction ids
