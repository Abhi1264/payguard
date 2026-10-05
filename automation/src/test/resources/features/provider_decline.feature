@negative @e2e @integration
Feature: Provider decline

  Scenario: Declined authorization leaves the order unpaid
    Given a registered user
    And an order for 25.00 USD
    And the provider will decline authorization
    When the user pays the order
    Then the payment is failed
    And the order is still created in the database
