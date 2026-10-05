package com.payguard.automation.fixtures;

import com.payguard.automation.models.CreatePaymentRequest;
import java.math.BigDecimal;
import java.util.UUID;

public final class PaymentFactory {

    private PaymentFactory() {
    }

    public static CreatePaymentRequest valid(UUID userId, UUID orderId) {
        return new CreatePaymentRequest(orderId, userId, OrderFactory.AMOUNT, OrderFactory.CURRENCY);
    }

    public static CreatePaymentRequest withAmount(UUID userId, UUID orderId, String amount) {
        return new CreatePaymentRequest(orderId, userId, new BigDecimal(amount), OrderFactory.CURRENCY);
    }

    public static CreatePaymentRequest withCurrency(UUID userId, UUID orderId, String currency) {
        return new CreatePaymentRequest(orderId, userId, OrderFactory.AMOUNT, currency);
    }

    public static CreatePaymentRequest negativeAmount(UUID userId, UUID orderId) {
        return new CreatePaymentRequest(orderId, userId, new BigDecimal("-5.00"), OrderFactory.CURRENCY);
    }
}
