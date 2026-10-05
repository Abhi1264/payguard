package com.payguard.automation.diagnostics;

import com.payguard.automation.database.DatabaseClient;
import java.util.UUID;

public final class Diagnostics {

    private static final ThreadLocal<UUID> ORDER = new ThreadLocal<>();
    private static final ThreadLocal<UUID> PAYMENT = new ThreadLocal<>();

    private Diagnostics() {
    }

    public static void track(UUID orderId, UUID paymentId) {
        if (orderId != null) {
            ORDER.set(orderId);
        }
        if (paymentId != null) {
            PAYMENT.set(paymentId);
        }
    }

    public static String renderAndClear() {
        UUID orderId = ORDER.get();
        UUID paymentId = PAYMENT.get();
        clear();
        if (orderId == null && paymentId == null) {
            return null;
        }
        DatabaseClient database = new DatabaseClient();
        StringBuilder text = new StringBuilder();
        if (paymentId != null) {
            text.append(database.describePayment(paymentId)).append('\n');
        }
        if (orderId != null) {
            text.append(database.describeOrder(orderId)).append('\n');
        }
        return text.toString();
    }

    public static void clear() {
        ORDER.remove();
        PAYMENT.remove();
    }
}
