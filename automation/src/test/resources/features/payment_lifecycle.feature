@smoke @e2e @integration
Feature: Payment lifecycle

  Scenario: User pays an order and receives a refund
    Given a registered user
    And an order for 25.00 USD
    When the user pays the order
    Then the payment is captured
    And the order is paid in the database
    When the user refunds the payment
    Then the payment is refunded
    And the order is refunded in the database
