@error
Feature: Error features

  Scenario Outline: Parsing errors
    Given user, who wants to test errors
    When user sends <request>
    Then error code <errCode> is returned
    Then error "<errDescription>" is returned

    Examples:
      | request                                          | errCode | errDescription                |
      | {"rum":"get_me"}                                 | 50      | Parse error, check input data |
      | {"run":"getme"}                                  | 50      | Parse error, check input data |
      | {"run":"get_me","address":"0001-00000000-9B6F"}  | 50      | Parse error, check input data |
