@non_existent
Feature: non-existent u/n

    Scenario: Send to user in non-existent node
        Given 1 user
        When sends 0.00000000001 ADS to user in non-existent node
        Then transfer to invalid address is rejected

    Scenario: Send to non-existent user in node
        Given 1 user
        When sends 0.00000000001 ADS to non-existent user in node
        Then transfer to invalid address is rejected

    Scenario: Retrieve funds from user in non-existent node
        Given 1 user
        When retrieves from user in non-existent node
        Then transfer to invalid address is rejected

    Scenario: Retrieve funds from non-existent user in node
        Given 1 user
        When retrieves from non-existent user in node
        Then transfer to invalid address is rejected

    Scenario: Check log
        Given user log

    Scenario: Check transaction
        Given transaction ids
