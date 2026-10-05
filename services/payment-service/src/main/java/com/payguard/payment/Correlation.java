package com.payguard.payment;

import java.util.UUID;
import org.slf4j.MDC;

public final class Correlation {

    public static final String HEADER = "X-Request-Id";

    private Correlation() {
    }

    public static String current() {
        String id = MDC.get("requestId");
        if (id == null || id.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return id;
    }
}
