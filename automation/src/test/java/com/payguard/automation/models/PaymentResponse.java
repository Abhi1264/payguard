package com.payguard.automation.models;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        UUID orderId,
        UUID userId,
        BigDecimal amount,
        String currency,
        String status,
        String providerTransactionId,
        Instant authorizedAt,
        Instant createdAt
) {
}
