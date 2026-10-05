package com.payguard.order;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class OrderTransitionsTest {

    @Test
    void payOnlyFromCreated() {
        assertTrue(OrderTransitions.canPay("CREATED"));
        assertFalse(OrderTransitions.canPay("PAID"));
        assertFalse(OrderTransitions.canPay("REFUNDED"));
    }

    @Test
    void refundOnlyFromPaid() {
        assertTrue(OrderTransitions.canRefund("PAID"));
        assertFalse(OrderTransitions.canRefund("CREATED"));
        assertFalse(OrderTransitions.canRefund("REFUNDED"));
    }
}
