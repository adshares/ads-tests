@node
Feature: Node features

  Scenario: Create node
    Given user, who wants to create node
    When user creates node
    Then node is created

  Scenario: Change node key
    Given main user, who wants to change own node key
    When user changes own node key
    Then node key is changed

  Scenario: Check log
    Given user log
