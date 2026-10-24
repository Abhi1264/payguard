package com.payguard.payment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.payguard.common.ApiException;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class IdempotencyTest {

    @Test
    void blankKeyIsAbsentAndLongKeyIsRejected() {
        assertNull(PaymentService.idempotencyKey(" "));
        assertEquals("abc", PaymentService.idempotencyKey(" abc "));
        ApiException rejected = assertThrows(ApiException.class, () -> PaymentService.idempotencyKey("x".repeat(65)));
        assertEquals(400, rejected.status());
    }

    @Test
    void replayRequiresTheSameCharge() {
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Payment payment = new Payment();
        payment.setOrderId(orderId);
        payment.setUserId(userId);
        payment.setAmount(new BigDecimal("25.00"));
        payment.setCurrency("USD");
        CreatePaymentRequest same = new CreatePaymentRequest(orderId, userId, new BigDecimal("25.00"), "USD");
        PaymentService.sameCharge(payment, same);
        CreatePaymentRequest different = new CreatePaymentRequest(UUID.randomUUID(), userId, new BigDecimal("25.00"), "USD");
        ApiException conflict = assertThrows(ApiException.class, () -> PaymentService.sameCharge(payment, different));
        assertEquals(409, conflict.status());
    }
}
