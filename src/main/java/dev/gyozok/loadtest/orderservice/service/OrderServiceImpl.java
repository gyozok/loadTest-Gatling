package dev.gyozok.loadtest.orderservice.service;

import dev.gyozok.loadtest.orderservice.dto.OrderRequest;
import dev.gyozok.loadtest.orderservice.dto.OrderResponse;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class OrderServiceImpl implements OrderService {
    @Override
    public Optional<OrderResponse> getOrder(Long id) {
        return Optional.empty();
    }

    @Override
    public OrderResponse createOrder(OrderRequest request) {
        return null;
    }
}
