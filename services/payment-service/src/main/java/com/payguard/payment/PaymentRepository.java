package com.payguard.payment;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    boolean existsByOrderIdAndStatusNot(UUID orderId, String status);

    Optional<Payment> findByIdempotencyKey(String idempotencyKey);
}
