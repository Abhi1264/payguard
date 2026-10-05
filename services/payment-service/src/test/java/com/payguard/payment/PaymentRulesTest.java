package com.payguard.payment;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PaymentRulesTest {

    @Test
    void refundOnlyCapturedPayments() {
        assertTrue(PaymentRules.canRefund("CAPTURED"));
        assertFalse(PaymentRules.canRefund("REFUNDED"));
        assertFalse(PaymentRules.canRefund("FAILED"));
        assertFalse(PaymentRules.canRefund("PENDING"));
        assertFalse(PaymentRules.canRefund("AUTHORIZED"));
    }
}
