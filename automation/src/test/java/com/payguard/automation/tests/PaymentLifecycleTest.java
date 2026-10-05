package com.payguard.automation.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.payguard.automation.assertions.PayGuardAssertions;
import com.payguard.automation.assertions.SchemaAssertions;
import com.payguard.automation.clients.ApiResult;
import com.payguard.automation.clients.PaymentApiClient;
import com.payguard.automation.diagnostics.Diagnostics;
import com.payguard.automation.fixtures.TestData;
import com.payguard.automation.fixtures.PaymentFactory;
import com.payguard.automation.fixtures.ProviderStubs;
import com.payguard.automation.models.OrderResponse;
import com.payguard.automation.models.PaymentResponse;
import com.payguard.automation.models.UserResponse;
import com.payguard.automation.support.PayGuardTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class PaymentLifecycleTest extends PayGuardTest {

    private final PaymentApiClient payments = new PaymentApiClient();
    private final TestData given = new TestData();

    @Test
    @Tag("smoke")
    @Tag("e2e")
    @Tag("integration")
    @Tag("contract")
    void shouldCompletePaymentAndRefundLifecycle() {
        UserResponse user = given.user();
        OrderResponse order = given.order(user.id());
        Diagnostics.track(order.id(), null);

        ApiResult created = payments.create(PaymentFactory.valid(user.id(), order.id()));
        assertEquals(201, created.status(), created.body());
        PaymentResponse payment = created.as(PaymentResponse.class);
        Diagnostics.track(order.id(), payment.id());
        assertEquals("CAPTURED", payment.status());
        assertNotNull(payment.providerTransactionId());
        assertNotNull(payment.authorizedAt());
        assertNotNull(created.requestId());
        SchemaAssertions.matches(created.response(), "schemas/payment.json");
        PayGuardAssertions.paymentStatus(payment.id(), "CAPTURED");
        PayGuardAssertions.orderStatus(order.id(), "PAID");

        ApiResult refunded = payments.refund(payment.id());
        assertEquals(200, refunded.status(), refunded.body());
        assertEquals("REFUNDED", refunded.as(PaymentResponse.class).status());
        SchemaAssertions.matches(refunded.response(), "schemas/refund.json");
        PayGuardAssertions.paymentStatus(payment.id(), "REFUNDED");
        PayGuardAssertions.orderStatus(order.id(), "REFUNDED");
    }

    @Test
    @Tag("negative")
    @Tag("e2e")
    @Tag("integration")
    void shouldLeaveOrderCreatedWhenProviderDeclines() {
        UserResponse user = given.user();
        OrderResponse order = given.order(user.id());
        Diagnostics.track(order.id(), null);
        ProviderStubs.declineAuthorize(order.id());

        ApiResult created = payments.create(PaymentFactory.valid(user.id(), order.id()));

        assertEquals(201, created.status(), created.body());
        PaymentResponse payment = created.as(PaymentResponse.class);
        Diagnostics.track(order.id(), payment.id());
        assertEquals("FAILED", payment.status());
        assertNull(payment.authorizedAt());
        assertNull(payment.providerTransactionId());
        PayGuardAssertions.paymentStatus(payment.id(), "FAILED");
        PayGuardAssertions.paymentNotAuthorized(payment.id());
        PayGuardAssertions.orderStatus(order.id(), "CREATED");
    }
}
