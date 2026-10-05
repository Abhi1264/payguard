package com.payguard.order;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orders;

    public OrderController(OrderService orders) {
        this.orders = orders;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> create(@Valid @RequestBody CreateOrderRequest request) {
        return ResponseEntity.status(201).body(orders.create(request));
    }

    @GetMapping("/{id}")
    public OrderResponse get(@PathVariable UUID id) {
        return orders.get(id);
    }

    @PostMapping("/{id}/pay")
    public OrderResponse pay(@PathVariable UUID id,
            @RequestHeader(value = "X-Fail-Order-Update", required = false) String failOrderUpdate) {
        // ponytail: test fault header, delete when order updates are retried by an outbox
        if ("true".equals(failOrderUpdate)) {
            throw new ApiException(500, "INTERNAL_ERROR", "Order update failed");
        }
        return orders.markPaid(id);
    }

    @PostMapping("/{id}/refund")
    public OrderResponse refund(@PathVariable UUID id) {
        return orders.markRefunded(id);
    }
}
