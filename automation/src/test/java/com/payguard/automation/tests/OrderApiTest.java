package com.payguard.automation.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.payguard.automation.assertions.PayGuardAssertions;
import com.payguard.automation.assertions.SchemaAssertions;
import com.payguard.automation.clients.ApiResult;
import com.payguard.automation.clients.OrderApiClient;
import com.payguard.automation.fixtures.TestData;
import com.payguard.automation.fixtures.OrderFactory;
import com.payguard.automation.models.OrderResponse;
import com.payguard.automation.models.UserResponse;
import com.payguard.automation.support.PayGuardTest;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class OrderApiTest extends PayGuardTest {

    private final OrderApiClient orders = new OrderApiClient();
    private final TestData given = new TestData();

    @Test
    @Tag("smoke")
    @Tag("contract")
    void shouldCreateOrderWithCreatedStatus() {
        UserResponse user = given.user();

        ApiResult result = orders.create(OrderFactory.validOrder(user.id()));

        assertEquals(201, result.status(), result.body());
        OrderResponse order = result.as(OrderResponse.class);
        assertEquals(user.id(), order.userId());
        assertEquals(0, OrderFactory.AMOUNT.compareTo(order.amount()));
        assertEquals("USD", order.currency());
        assertEquals("CREATED", order.status());
        assertNotNull(order.id());
        SchemaAssertions.matches(result.response(), "schemas/order.json");
    }

    @Test
    @Tag("regression")
    void shouldRetrieveOrder() {
        UserResponse user = given.user();
        OrderResponse created = given.order(user.id());

        ApiResult result = orders.get(created.id());

        assertEquals(200, result.status(), result.body());
        assertEquals("CREATED", result.as(OrderResponse.class).status());
    }

    @Test
    @Tag("negative")
    void shouldRejectOrderForUnknownUser() {
        PayGuardAssertions.error(orders.create(OrderFactory.validOrder(UUID.randomUUID())), 404, "NOT_FOUND");
    }

    @Test
    @Tag("negative")
    void shouldRejectNegativeOrderAmount() {
        UserResponse user = given.user();
        PayGuardAssertions.error(orders.create(OrderFactory.negativeAmount(user.id())), 400, "VALIDATION_ERROR");
    }

    @Test
    @Tag("negative")
    void shouldRejectInvalidCurrency() {
        UserResponse user = given.user();
        PayGuardAssertions.error(orders.create(OrderFactory.invalidCurrency(user.id())), 400, "VALIDATION_ERROR");
    }

    @Test
    @Tag("negative")
    void shouldReturnNotFoundForUnknownOrder() {
        PayGuardAssertions.error(orders.get(UUID.randomUUID()), 404, "NOT_FOUND");
    }
}
