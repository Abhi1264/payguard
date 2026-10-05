package com.payguard.automation.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.payguard.automation.assertions.PayGuardAssertions;
import com.payguard.automation.assertions.SchemaAssertions;
import com.payguard.automation.clients.ApiResult;
import com.payguard.automation.clients.OrderApiClient;
import com.payguard.automation.clients.PaymentApiClient;
import com.payguard.automation.fixtures.TestData;
import com.payguard.automation.fixtures.PaymentFactory;
import com.payguard.automation.fixtures.ProviderStubs;
import com.payguard.automation.models.OrderResponse;
import com.payguard.automation.models.PaymentResponse;
import com.payguard.automation.models.UserResponse;
import com.payguard.automation.support.PayGuardTest;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class PaymentApiTest extends PayGuardTest {

    private final PaymentApiClient payments = new PaymentApiClient();
    private final OrderApiClient orders = new OrderApiClient();
    private final TestData given = new TestData();

    @Test
    @Tag("regression")
    @Tag("contract")
    void shouldRetrieveCapturedPayment() {
        PaymentResponse created = captured();

        ApiResult result = payments.get(created.id());

        assertEquals(200, result.status(), result.body());
        assertEquals("CAPTURED", result.as(PaymentResponse.class).status());
        SchemaAssertions.matches(result.response(), "schemas/payment.json");
    }

    @Test
    @Tag("negative")
    void shouldRejectPaymentForUnknownUser() {
        UserResponse user = given.user();
        OrderResponse order = given.order(user.id());

        PayGuardAssertions.error(
                payments.create(PaymentFactory.valid(UUID.randomUUID(), order.id())),
                404,
                "NOT_FOUND");
    }

    @Test
    @Tag("negative")
    void shouldRejectPaymentForUnknownOrder() {
        UserResponse user = given.user();

        PayGuardAssertions.error(
                payments.create(PaymentFactory.valid(user.id(), UUID.randomUUID())),
                404,
                "NOT_FOUND");
    }

    @Test
    @Tag("negative")
    void shouldRejectPaymentWhenAmountDoesNotMatchOrder() {
        UserResponse user = given.user();
        OrderResponse order = given.order(user.id());

        PayGuardAssertions.error(
                payments.create(PaymentFactory.withAmount(user.id(), order.id(), "10.00")),
                409,
                "CONFLICT");
    }

    @Test
    @Tag("negative")
    void shouldRejectPaymentWhenCurrencyDoesNotMatchOrder() {
        UserResponse user = given.user();
        OrderResponse order = given.order(user.id());

        PayGuardAssertions.error(
                payments.create(PaymentFactory.withCurrency(user.id(), order.id(), "EUR")),
                409,
                "CONFLICT");
    }

    @Test
    @Tag("negative")
    void shouldRejectNegativePaymentAmount() {
        PayGuardAssertions.error(
                payments.create(PaymentFactory.negativeAmount(UUID.randomUUID(), UUID.randomUUID())),
                400,
                "VALIDATION_ERROR");
    }

    @Test
    @Tag("negative")
    @Tag("regression")
    void shouldRejectPaymentWhenOrderIsAlreadyPaid() {
        PaymentResponse payment = captured();

        PayGuardAssertions.error(payments.create(PaymentFactory.valid(payment.userId(), payment.orderId())), 409, "CONFLICT");
    }

    @Test
    @Tag("regression")
    void shouldAllowAnotherPaymentAfterFailure() {
        UserResponse user = given.user();
        OrderResponse order = given.order(user.id());
        ProviderStubs.declineAuthorize(order.id());
        ApiResult first = payments.create(PaymentFactory.valid(user.id(), order.id()));
        ApiResult second = payments.create(PaymentFactory.valid(user.id(), order.id()));

        assertEquals("FAILED", first.as(PaymentResponse.class).status());
        assertEquals(201, second.status(), second.body());
        assertEquals("FAILED", second.as(PaymentResponse.class).status());
    }

    @Test
    @Tag("negative")
    @Tag("regression")
    void shouldLeavePaymentCapturedWhenProviderRefundFails() {
        PaymentResponse payment = captured();
        ProviderStubs.failRefund(payment.orderId());

        PayGuardAssertions.error(payments.refund(payment.id()), 502, "INTERNAL_ERROR");
        PayGuardAssertions.paymentStatus(payment.id(), "CAPTURED");
        PayGuardAssertions.orderStatus(payment.orderId(), "PAID");
    }

    @Test
    @Tag("negative")
    @Tag("regression")
    void shouldReverseChargeWhenOrderUpdateFails() {
        UserResponse user = given.user();
        OrderResponse order = given.order(user.id());

        ApiResult result = payments.createThatFailsOrderUpdate(PaymentFactory.valid(user.id(), order.id()));

        assertEquals(201, result.status(), result.body());
        assertEquals("FAILED", result.as(PaymentResponse.class).status());
        PayGuardAssertions.orderStatus(order.id(), "CREATED");
    }

    @Test
    @Tag("negative")
    @Tag("regression")
    void shouldRejectRefundOfRefundedPayment() {
        PaymentResponse payment = captured();
        assertEquals(200, payments.refund(payment.id()).status());

        PayGuardAssertions.error(payments.refund(payment.id()), 409, "CONFLICT");
    }

    @Test
    @Tag("negative")
    void shouldRejectInvalidPaymentId() {
        PayGuardAssertions.error(payments.get("not-a-uuid"), 400, "VALIDATION_ERROR");
        PayGuardAssertions.error(payments.refund("not-a-uuid"), 400, "VALIDATION_ERROR");
    }

    @Test
    @Tag("negative")
    void shouldReturnNotFoundForUnknownPayment() {
        UUID id = UUID.randomUUID();
        PayGuardAssertions.error(payments.get(id), 404, "NOT_FOUND");
        PayGuardAssertions.error(payments.refund(id), 404, "NOT_FOUND");
    }

    @Test
    @Tag("negative")
    void shouldRejectMalformedPaymentRequest() {
        PayGuardAssertions.error(payments.postRaw("{"), 400, "VALIDATION_ERROR");
    }

    @Test
    @Tag("negative")
    void shouldRejectPaymentMissingRequiredFields() {
        PayGuardAssertions.error(payments.postRaw("{}"), 400, "VALIDATION_ERROR");
    }

    @Test
    @Tag("negative")
    @Tag("regression")
    void shouldRecordFailedPaymentWhenProviderReturnsServerError() {
        UserResponse user = given.user();
        OrderResponse order = given.order(user.id());
        ProviderStubs.failCapture(order.id());

        ApiResult result = payments.create(PaymentFactory.valid(user.id(), order.id()));

        assertEquals(201, result.status(), result.body());
        PaymentResponse payment = result.as(PaymentResponse.class);
        assertEquals("FAILED", payment.status());
        assertNotNull(payment.authorizedAt());
        assertEquals("CREATED", orders.get(order.id()).as(OrderResponse.class).status());
    }

    @Test
    @Tag("negative")
    @Tag("regression")
    void shouldRecordFailedPaymentWhenProviderTimesOut() {
        UserResponse user = given.user();
        OrderResponse order = given.order(user.id());
        ProviderStubs.delayAuthorize(order.id());

        ApiResult result = payments.create(PaymentFactory.valid(user.id(), order.id()));

        assertEquals(201, result.status(), result.body());
        PaymentResponse payment = result.as(PaymentResponse.class);
        assertEquals("FAILED", payment.status());
        assertNull(payment.authorizedAt());
        assertEquals("CREATED", orders.get(order.id()).as(OrderResponse.class).status());
    }

    private PaymentResponse captured() {
        UserResponse user = given.user();
        OrderResponse order = given.order(user.id());
        ApiResult result = payments.create(PaymentFactory.valid(user.id(), order.id()));
        assertEquals(201, result.status(), result.body());
        PaymentResponse payment = result.as(PaymentResponse.class);
        assertEquals("CAPTURED", payment.status());
        return payment;
    }
}
