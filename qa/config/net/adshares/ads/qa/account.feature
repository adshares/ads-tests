@account
Feature: Account features

  Scenario: Create account
    Given user, who wants to create account
    When user creates account
    Then account is created

  Scenario: Create remote account
    Given user, who wants to create account
    When user creates remote account
    Then account is created

  Scenario: Create remote account (dividend block)
    Given user, who wants to create account
    When user creates remote account in dividend block
    Then account is created

  Scenario: Create account (custom key)
    Given user, who wants to create account
    When user creates account with custom key
    Then account is created

  Scenario: Create remote account (custom key)
    Given user, who wants to create account
    When user creates remote account with custom key
    Then account is created

  Scenario: Create remote account (custom key, dividend block)
    Given user, who wants to create account
    When user creates remote account with custom key in dividend block
    Then account is created

  Scenario: Change account key
    Given user, who wants to change key
    When user changes key
    Then he cannot use old key
    And transaction can be authorised with new key
    And account change key transaction is present in log

  Scenario: Check log
    Given user log

  Scenario: Check transaction
    Given transaction ids
