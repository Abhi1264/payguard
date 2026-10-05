package com.payguard.automation.fixtures;

import com.payguard.automation.clients.ApiResult;
import com.payguard.automation.clients.OrderApiClient;
import com.payguard.automation.clients.UserApiClient;
import com.payguard.automation.models.OrderResponse;
import com.payguard.automation.models.UserResponse;
import java.util.UUID;

public final class TestData {

    private final UserApiClient users = new UserApiClient();
    private final OrderApiClient orders = new OrderApiClient();

    public UserResponse user() {
        ApiResult result = users.create(UserFactory.validUser());
        if (result.status() != 201) {
            throw new AssertionError("user setup failed: " + result.status() + " " + result.body());
        }
        return result.as(UserResponse.class);
    }

    public OrderResponse order(UUID userId) {
        ApiResult result = orders.create(OrderFactory.validOrder(userId));
        if (result.status() != 201) {
            throw new AssertionError("order setup failed: " + result.status() + " " + result.body());
        }
        return result.as(OrderResponse.class);
    }
}
