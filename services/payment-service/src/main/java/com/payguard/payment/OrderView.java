package com.payguard.payment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderView(
        UUID id,
        UUID userId,
        BigDecimal amount,
        String currency,
        String status,
        Instant createdAt
) {
}
