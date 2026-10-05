package com.payguard.automation.steps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.payguard.automation.assertions.PayGuardAssertions;
import com.payguard.automation.clients.ApiResult;
import com.payguard.automation.clients.PaymentApiClient;
import com.payguard.automation.diagnostics.Diagnostics;
import com.payguard.automation.diagnostics.ExchangeLog;
import com.payguard.automation.fixtures.TestData;
import com.payguard.automation.fixtures.PaymentFactory;
import com.payguard.automation.fixtures.ProviderStubs;
import com.payguard.automation.models.OrderResponse;
import com.payguard.automation.models.PaymentResponse;
import com.payguard.automation.models.UserResponse;
import com.payguard.automation.support.PayGuardExtension;
import io.cucumber.java.After;
import io.cucumber.java.BeforeAll;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.nio.charset.StandardCharsets;

public class LifecycleSteps {

    private final TestData given = new TestData();
    private final PaymentApiClient payments = new PaymentApiClient();
    private UserResponse user;
    private OrderResponse order;
    private PaymentResponse payment;

    @BeforeAll
    public static void startStack() {
        PayGuardExtension.ensureStarted();
    }

    @After
    public void attachOnFailure(Scenario scenario) {
        if (!scenario.isFailed()) {
            ExchangeLog.clear();
            Diagnostics.clear();
            return;
        }
        String http = ExchangeLog.renderAndClear();
        if (http != null) {
            scenario.attach(http.getBytes(StandardCharsets.UTF_8), "text/plain", "HTTP exchange");
        }
        String database = Diagnostics.renderAndClear();
        if (database != null) {
            scenario.attach(database.getBytes(StandardCharsets.UTF_8), "text/plain", "Database state");
        }
    }

    @Given("a registered user")
    public void registeredUser() {
        user = given.user();
    }

    @Given("an order for 25.00 USD")
    public void order() {
        order = given.order(user.id());
        Diagnostics.track(order.id(), null);
    }

    @Given("the provider will decline authorization")
    public void decline() {
        ProviderStubs.declineAuthorize(order.id());
    }

    @When("the user pays the order")
    public void pay() {
        ApiResult result = payments.create(PaymentFactory.valid(user.id(), order.id()));
        assertEquals(201, result.status(), result.body());
        payment = result.as(PaymentResponse.class);
        Diagnostics.track(order.id(), payment.id());
    }

    @When("the user refunds the payment")
    public void refund() {
        ApiResult result = payments.refund(payment.id());
        assertEquals(200, result.status(), result.body());
        payment = result.as(PaymentResponse.class);
    }

    @Then("the payment is captured")
    public void captured() {
        assertEquals("CAPTURED", payment.status());
        PayGuardAssertions.paymentStatus(payment.id(), "CAPTURED");
    }

    @Then("the payment is failed")
    public void failed() {
        assertEquals("FAILED", payment.status());
        assertNull(payment.authorizedAt());
        PayGuardAssertions.paymentStatus(payment.id(), "FAILED");
    }

    @Then("the payment is refunded")
    public void refunded() {
        assertEquals("REFUNDED", payment.status());
        PayGuardAssertions.paymentStatus(payment.id(), "REFUNDED");
    }

    @Then("the order is paid in the database")
    public void orderPaid() {
        PayGuardAssertions.orderStatus(order.id(), "PAID");
    }

    @Then("the order is refunded in the database")
    public void orderRefunded() {
        PayGuardAssertions.orderStatus(order.id(), "REFUNDED");
    }

    @Then("the order is still created in the database")
    public void orderCreated() {
        PayGuardAssertions.orderStatus(order.id(), "CREATED");
    }
}
