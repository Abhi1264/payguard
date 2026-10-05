package com.payguard.automation.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.payguard.automation.assertions.PayGuardAssertions;
import com.payguard.automation.config.Config;
import io.restassured.RestAssured;
import com.payguard.automation.assertions.SchemaAssertions;
import com.payguard.automation.clients.ApiResult;
import com.payguard.automation.clients.UserApiClient;
import com.payguard.automation.fixtures.UserFactory;
import com.payguard.automation.models.CreateUserRequest;
import com.payguard.automation.models.UserResponse;
import com.payguard.automation.support.PayGuardTest;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class UserApiTest extends PayGuardTest {

    private final UserApiClient users = new UserApiClient();

    @Test
    @Tag("smoke")
    @Tag("contract")
    void shouldCreateValidUser() {
        CreateUserRequest request = UserFactory.validUser();
        ApiResult result = users.create(request);

        assertEquals(201, result.status(), result.body());
        UserResponse user = result.as(UserResponse.class);
        assertEquals(request.name(), user.name());
        assertEquals(request.email(), user.email());
        assertNotNull(user.id());
        assertNotNull(user.createdAt());
        assertNotNull(result.requestId());
        SchemaAssertions.matches(result.response(), "schemas/user.json");
    }

    @Test
    @Tag("regression")
    void shouldRetrieveExistingUser() {
        UserResponse created = users.create(UserFactory.validUser()).as(UserResponse.class);

        ApiResult result = users.get(created.id());

        assertEquals(200, result.status(), result.body());
        UserResponse fetched = result.as(UserResponse.class);
        assertEquals(created.id(), fetched.id());
        assertEquals(created.email(), fetched.email());
    }

    @Test
    @Tag("negative")
    void shouldRejectMissingName() {
        PayGuardAssertions.error(users.create(UserFactory.missingName()), 400, "VALIDATION_ERROR");
    }

    @Test
    @Tag("negative")
    void shouldRejectInvalidEmail() {
        PayGuardAssertions.error(users.create(UserFactory.invalidEmail()), 400, "VALIDATION_ERROR");
    }

    @Test
    @Tag("negative")
    @Tag("regression")
    void shouldRejectDuplicateEmail() {
        CreateUserRequest first = UserFactory.validUser();
        assertEquals(201, users.create(first).status());

        ApiResult duplicate = users.create(new CreateUserRequest("Grace Hopper", first.email()));

        PayGuardAssertions.error(duplicate, 409, "CONFLICT");
    }

    @Test
    @Tag("negative")
    void shouldRejectMissingToken() {
        int status = RestAssured.given()
                .baseUri(Config.userServiceUrl())
                .get("/users/" + UUID.randomUUID())
                .statusCode();
        assertEquals(401, status);
    }

    @Test
    @Tag("negative")
    void shouldReturnNotFoundForUnknownUser() {
        PayGuardAssertions.error(users.get(UUID.randomUUID()), 404, "NOT_FOUND");
    }
}
