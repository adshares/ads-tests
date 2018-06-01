@account
Feature: Account features

  Scenario: Change account key
    Given user, who wants to change key
    When user changes key
    Then he cannot use old key
    And transaction can be authorised with new key
    And account change key transaction is present in log

  Scenario: Check log
    Given user log
