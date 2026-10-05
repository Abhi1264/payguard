package com.payguard.automation.models;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateOrderRequest(UUID userId, BigDecimal amount, String currency) {
}
