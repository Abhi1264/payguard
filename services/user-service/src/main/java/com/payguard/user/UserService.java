package com.payguard.user;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository users;

    public UserService(UserRepository users) {
        this.users = users;
    }

    public UserResponse create(CreateUserRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (users.existsByEmailIgnoreCase(email)) {
            throw new ApiException(409, "CONFLICT", "Email already exists");
        }
        UserAccount user = new UserAccount();
        user.setId(UUID.randomUUID());
        user.setName(request.name().trim());
        user.setEmail(email);
        user.setCreatedAt(Instant.now());
        try {
            return UserResponse.from(users.saveAndFlush(user));
        } catch (DataIntegrityViolationException ex) {
            throw new ApiException(409, "CONFLICT", "Email already exists");
        }
    }

    public UserResponse get(UUID id) {
        return users.findById(id)
                .map(UserResponse::from)
                .orElseThrow(() -> new ApiException(404, "NOT_FOUND", "User not found"));
    }
}
