package com.payguard.automation.models;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        UUID userId,
        BigDecimal amount,
        String currency,
        String status,
        Instant createdAt
) {
}
