package dev.gyozok.loadtest.orderservice.service;

import dev.gyozok.loadtest.orderservice.dto.OrderRequest;
import dev.gyozok.loadtest.orderservice.dto.OrderResponse;
import dev.gyozok.loadtest.orderservice.event.OrderCreatedEvent;
import dev.gyozok.loadtest.orderservice.model.Order;
import dev.gyozok.loadtest.orderservice.model.OrderStatus;
import dev.gyozok.loadtest.orderservice.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

@Service
public class OrderServiceImpl implements OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);
    @Value("${app.kafka.topic.order-events:order-create-events}")
    private static String ORDER_EVENTS_TOPIC;

    private final OrderRepository orderRepository;
    private final KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate;

    public OrderServiceImpl(OrderRepository orderRepository, KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate) {
        this.orderRepository = orderRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public Optional<OrderResponse> getOrder(Long id) {
        return orderRepository.findById(id)
                .map(this::toResponse);
    }

    @Override
    public OrderResponse createOrder(OrderRequest request) {
        //1. Persist the request with status PENDING
        Order order = new Order(
                request.item(),
                request.quantity(),
                OrderStatus.PENDING,
                Instant.now()
        );
        Order saved = orderRepository.save(order);

        //2. Publish the event, the key is the orderId to land in the same partition
        OrderCreatedEvent event = new OrderCreatedEvent(
                saved.getId(),
                saved.getItem(),
                saved.getQuantity(),
                saved.getCreatedAt()
        );
        try {
            kafkaTemplate.send(
              ORDER_EVENTS_TOPIC, //topic
                    saved.getId().toString(), //key
                    event
            );
        } catch (Exception ex) {
            //because the event is already saved we can retry later
            //it will be logged here, so it will be seen in the metrics
            log.error("Failed to publish OrderCreatedEvent for order {}", saved.getId(), ex);
        }
        return toResponse(saved);
    }

    private OrderResponse toResponse(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getItem(),
                order.getQuantity(),
                order.getStatus(),
                order.getCreatedAt()
        );
    }
}
