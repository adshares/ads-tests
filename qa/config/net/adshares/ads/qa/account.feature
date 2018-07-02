@account
Feature: Account features

  Scenario: Change account key
    Given user, who wants to change key
    When user changes key
    Then he cannot use old key
    And transaction can be authorised with new key
    And account change key transaction is present in log

  Scenario: Create account
    Given user, who wants to create account
    When user creates account
    Then account is created

  Scenario: Create remote account
    Given user, who wants to create account
    When user creates remote account
    Then account is created

  Scenario: Check log
    Given user log

  Scenario: Check transaction
    Given transaction ids
