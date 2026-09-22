package dev.gyozok.loadtest.orderservice.dto;

import dev.gyozok.loadtest.orderservice.model.OrderStatus;

import java.time.Instant;

public record OrderResponse(
        Long id,
        String item,
        int quantity,
        //status show is the request already processed or not
        OrderStatus status,
        Instant createdAt
) {
}
