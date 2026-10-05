package com.payguard.payment;

import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository payments;
    private final UserClient users;
    private final OrderClient orders;
    private final ProviderClient provider;

    public PaymentService(PaymentRepository payments, UserClient users, OrderClient orders, ProviderClient provider) {
        this.payments = payments;
        this.users = users;
        this.orders = orders;
        this.provider = provider;
    }

    public PaymentResponse create(CreatePaymentRequest request) {
        users.requireExists(request.userId());
        OrderView order = orders.get(request.orderId());
        if (!order.userId().equals(request.userId())) {
            throw new ApiException(409, "CONFLICT", "Order does not belong to user");
        }
        if (order.amount().compareTo(request.amount()) != 0) {
            throw new ApiException(409, "CONFLICT", "Payment amount does not match order");
        }
        if (!order.currency().equals(request.currency())) {
            throw new ApiException(409, "CONFLICT", "Payment currency does not match order");
        }
        if (!"CREATED".equals(order.status())) {
            throw new ApiException(409, "CONFLICT", "Order cannot be paid");
        }
        if (payments.existsByOrderIdAndStatusNot(request.orderId(), PaymentRules.FAILED)) {
            throw new ApiException(409, "CONFLICT", "Order already has a payment");
        }

        Payment payment = new Payment();
        payment.setId(UUID.randomUUID());
        payment.setOrderId(request.orderId());
        payment.setUserId(request.userId());
        payment.setAmount(request.amount());
        payment.setCurrency(request.currency());
        payment.setStatus(PaymentRules.PENDING);
        payment.setCreatedAt(Instant.now());
        try {
            payments.saveAndFlush(payment);
        } catch (DataIntegrityViolationException ex) {
            throw new ApiException(409, "CONFLICT", "Order already has a payment");
        }

        try {
            ProviderClient.AuthorizeResponse authorized = provider.authorize(payment);
            payment.setProviderTransactionId(authorized.transactionId());
            payment.setAuthorizedAt(Instant.now());
            payment.setStatus(PaymentRules.AUTHORIZED);
            payments.saveAndFlush(payment);
            provider.capture(payment);
            payment.setStatus(PaymentRules.CAPTURED);
            payments.saveAndFlush(payment);
        } catch (ProviderException ex) {
            log.warn("provider call failed for payment {}: {}", payment.getId(), ex.getMessage());
            payment.setStatus(PaymentRules.FAILED);
            payments.saveAndFlush(payment);
            return PaymentResponse.from(payment);
        }

        try {
            orders.markPaid(payment.getOrderId());
        } catch (ApiException ex) {
            log.warn("order update failed for payment {}: {}", payment.getId(), ex.getMessage());
            compensate(payment);
        }
        return PaymentResponse.from(payment);
    }

    private void compensate(Payment payment) {
        try {
            provider.refund(payment);
        } catch (ProviderException ex) {
            log.warn("compensating refund failed for payment {}: {}", payment.getId(), ex.getMessage());
            throw new ApiException(502, "INTERNAL_ERROR", "Order was not updated and the charge could not be reversed");
        }
        payment.setStatus(PaymentRules.FAILED);
        payments.saveAndFlush(payment);
    }

    public PaymentResponse get(UUID id) {
        return PaymentResponse.from(find(id));
    }

    public PaymentResponse refund(UUID id) {
        Payment payment = find(id);
        if (!PaymentRules.canRefund(payment.getStatus())) {
            throw new ApiException(409, "CONFLICT", "Payment cannot be refunded");
        }
        try {
            provider.refund(payment);
        } catch (ProviderException ex) {
            log.warn("provider refund failed for payment {}: {}", payment.getId(), ex.getMessage());
            throw new ApiException(502, "INTERNAL_ERROR", "Payment provider could not refund");
        }
        payment.setStatus(PaymentRules.REFUNDED);
        payments.saveAndFlush(payment);
        orders.markRefunded(payment.getOrderId());
        return PaymentResponse.from(payment);
    }

    private Payment find(UUID id) {
        return payments.findById(id).orElseThrow(() -> new ApiException(404, "NOT_FOUND", "Payment not found"));
    }
}
