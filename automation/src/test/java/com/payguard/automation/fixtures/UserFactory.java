package com.payguard.automation.fixtures;

import com.payguard.automation.models.CreateUserRequest;
import java.util.UUID;

public final class UserFactory {

    private UserFactory() {
    }

    public static CreateUserRequest validUser() {
        return new CreateUserRequest("Ada Lovelace", "user-" + UUID.randomUUID() + "@payguard.test");
    }

    public static CreateUserRequest missingName() {
        return new CreateUserRequest(" ", validUser().email());
    }

    public static CreateUserRequest invalidEmail() {
        return new CreateUserRequest("Ada Lovelace", "not-an-email");
    }
}
