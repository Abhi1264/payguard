package com.payguard.automation.assertions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.payguard.automation.clients.ApiResult;
import com.payguard.automation.database.DatabaseClient;
import com.payguard.automation.models.ErrorResponse;
import java.util.UUID;

public final class PayGuardAssertions {

    private PayGuardAssertions() {
    }

    public static void error(ApiResult result, int status, String code) {
        assertEquals(status, result.status(), result.body());
        ErrorResponse error = result.as(ErrorResponse.class);
        assertEquals(status, error.status());
        assertEquals(code, error.error());
        assertNotNull(error.message());
        assertFalse(error.message().isBlank());
        assertNotNull(error.path());
        assertNotNull(result.requestId());
    }

    public static void paymentStatus(UUID id, String expected) {
        assertEquals(expected, new DatabaseClient().paymentStatus(id), "payment " + id);
    }

    public static void orderStatus(UUID id, String expected) {
        assertEquals(expected, new DatabaseClient().orderStatus(id), "order " + id);
    }

    public static void paymentNotAuthorized(UUID id) {
        assertNull(new DatabaseClient().paymentAuthorizedAt(id), "payment " + id + " authorized_at");
    }
}
