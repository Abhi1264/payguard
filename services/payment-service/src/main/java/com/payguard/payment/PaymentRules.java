package com.payguard.payment;

public final class PaymentRules {

    public static final String PENDING = "PENDING";
    public static final String AUTHORIZED = "AUTHORIZED";
    public static final String CAPTURED = "CAPTURED";
    public static final String FAILED = "FAILED";
    public static final String REFUNDED = "REFUNDED";

    private PaymentRules() {
    }

    public static boolean canRefund(String status) {
        return CAPTURED.equals(status);
    }
}
