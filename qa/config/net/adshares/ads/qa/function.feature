@function
Feature: Function

  @get_accounts
  Scenario: Check get_accounts
    Given user, who will check get_accounts function
    When user calls get_accounts function
    Then response contains all accounts in node

  @log_account
  Scenario: Check log_account
    Given user, who will check log_account function
    When user calls log_account function
    Then user is able to get signature

  @extra_data
  Scenario: Check extra data
    Given user, who will check extra_data parameter
    When user call all functions with extra data
    Then no error is present in response
    And node works
