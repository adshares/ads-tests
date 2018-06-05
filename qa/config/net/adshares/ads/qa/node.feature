@node
Feature: Node features

  Scenario: Create node
    Given user, who wants to create node
    When user creates node
    Then node is created

  Scenario: Check log
    Given user log
