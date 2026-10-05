package com.payguard.order;

public final class OrderTransitions {

    public static final String CREATED = "CREATED";
    public static final String PAID = "PAID";
    public static final String REFUNDED = "REFUNDED";

    private OrderTransitions() {
    }

    public static boolean canPay(String status) {
        return CREATED.equals(status);
    }

    public static boolean canRefund(String status) {
        return PAID.equals(status);
    }
}
