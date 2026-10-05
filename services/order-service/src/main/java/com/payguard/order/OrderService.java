package com.payguard.order;

import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class OrderService {

    private final OrderRepository orders;
    private final UserClient users;

    public OrderService(OrderRepository orders, UserClient users) {
        this.orders = orders;
        this.users = users;
    }

    public OrderResponse create(CreateOrderRequest request) {
        users.requireExists(request.userId());
        CustomerOrder order = new CustomerOrder();
        order.setId(UUID.randomUUID());
        order.setUserId(request.userId());
        order.setAmount(request.amount());
        order.setCurrency(request.currency());
        order.setStatus(OrderTransitions.CREATED);
        order.setCreatedAt(Instant.now());
        return OrderResponse.from(orders.saveAndFlush(order));
    }

    public OrderResponse get(UUID id) {
        return OrderResponse.from(find(id));
    }

    public OrderResponse markPaid(UUID id) {
        CustomerOrder order = find(id);
        if (!OrderTransitions.canPay(order.getStatus())) {
            throw new ApiException(409, "CONFLICT", "Order cannot be paid");
        }
        order.setStatus(OrderTransitions.PAID);
        return OrderResponse.from(orders.saveAndFlush(order));
    }

    public OrderResponse markRefunded(UUID id) {
        CustomerOrder order = find(id);
        if (!OrderTransitions.canRefund(order.getStatus())) {
            throw new ApiException(409, "CONFLICT", "Order cannot be refunded");
        }
        order.setStatus(OrderTransitions.REFUNDED);
        return OrderResponse.from(orders.saveAndFlush(order));
    }

    private CustomerOrder find(UUID id) {
        return orders.findById(id).orElseThrow(() -> new ApiException(404, "NOT_FOUND", "Order not found"));
    }
}
