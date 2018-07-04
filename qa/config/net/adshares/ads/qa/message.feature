@message
Feature: Message

  Scenario: Check messages from last blocks
    Given user, who will check message
    When user checks all messages from last 10 block(s)
    Then he is able to get information about every transaction from message
