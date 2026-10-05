package com.payguard.automation.fixtures;

import com.payguard.automation.models.CreateOrderRequest;
import java.math.BigDecimal;
import java.util.UUID;

public final class OrderFactory {

    public static final BigDecimal AMOUNT = new BigDecimal("25.00");
    public static final String CURRENCY = "USD";

    private OrderFactory() {
    }

    public static CreateOrderRequest validOrder(UUID userId) {
        return new CreateOrderRequest(userId, AMOUNT, CURRENCY);
    }

    public static CreateOrderRequest negativeAmount(UUID userId) {
        return new CreateOrderRequest(userId, new BigDecimal("-1.00"), CURRENCY);
    }

    public static CreateOrderRequest invalidCurrency(UUID userId) {
        return new CreateOrderRequest(userId, AMOUNT, "XYZ");
    }
}
