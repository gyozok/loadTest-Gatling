package dev.gyozok.loadtest.orderservice.service;

import dev.gyozok.loadtest.orderservice.dto.OrderRequest;
import dev.gyozok.loadtest.orderservice.dto.OrderResponse;

import java.util.Optional;

public interface OrderService {

    public Optional<OrderResponse> getOrder(Long id);
    public OrderResponse createOrder(OrderRequest request);
}
