package dev.gyozok.loadtest.orderservice.service;

import dev.gyozok.loadtest.orderservice.dto.OrderRequest;
import dev.gyozok.loadtest.orderservice.dto.OrderResponse;
import dev.gyozok.loadtest.orderservice.event.OrderCreatedEvent;
import dev.gyozok.loadtest.orderservice.model.Order;
import dev.gyozok.loadtest.orderservice.model.OrderStatus;
import dev.gyozok.loadtest.orderservice.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.awt.geom.RectangularShape;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class OrderServiceImplTest {
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate;

    @InjectMocks
    private OrderServiceImpl sut;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(sut, "ORDER_EVENTS_TOPIC", "order-create-events");
    }

    @Test
    void createOrder_persistReturnsOrderWithPendingStatus() {
        // GIVEN
        OrderRequest request = new OrderRequest("item1", 2);
        stubSaveToAssignId(1L);

        // WHEN
        OrderResponse response = sut.createOrder(request);

        // THEN
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.item()).isEqualTo("item1");
        assertThat(response.quantity()).isEqualTo(2);
        assertThat(response.status()).isEqualTo(OrderStatus.PENDING);
        assertThat(response.createdAt()).isNotNull();
    }

    @Test
    void createOrder_publishesEventToConfiguredTopicKeyedByOrderId() {
        // GIVEN
        OrderRequest request = new OrderRequest("item2", 3);
        stubSaveToAssignId(42L);

        // WHEN
        sut.createOrder(request);

        // THEN
        ArgumentCaptor<OrderCreatedEvent> eventCaptor = ArgumentCaptor.forClass(OrderCreatedEvent.class);
        verify(kafkaTemplate).send(eq("order-create-events"), eq("42"), eventCaptor.capture());

        OrderCreatedEvent published = eventCaptor.getValue();
        assertThat(published.orderId()).isEqualTo(42L);
        assertThat(published.item()).isEqualTo("item2");
        assertThat(published.quantity()).isEqualTo(3);
    }

    @Test
    public void createOrder_stillReturnsResponseWhenKafkaPublishFails() {
        // GIVEN
        OrderRequest request = new OrderRequest("item3", 1);
        stubSaveToAssignId(11L);
        when(kafkaTemplate.send(anyString(), anyString(), any(OrderCreatedEvent.class)))
                .thenThrow(new RuntimeException("broker unreachable"));

        // WHEN
        OrderResponse response = sut.createOrder(request);

        // THEN
        assertThat(response.id()).isEqualTo(11L);
        assertThat(response.status()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    void getOrder_returnsMappedResponseWhenFound() {
        // GIVEN
        Order order = new Order("widget", 2, OrderStatus.CONFIRMED, java.time.Instant.now());
        ReflectionTestUtils.setField(order, "id", 7L);
        when(orderRepository.findById(7L)).thenReturn(Optional.of(order));

        // WHEN
        Optional<OrderResponse> result = sut.getOrder(7L);

        // THEN
        assertThat(result).isPresent();
        assertThat(result.get().id()).isEqualTo(7L);
        assertThat(result.get().status()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    void getOrderReturnsEmptyWhenNotFound() {
        // GIVEN
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        // WHEN
        Optional<OrderResponse> response = sut.getOrder(999L);

        // THEN
        assertThat(response).isEmpty();
    }

    private void stubSaveToAssignId(long id) {
        when(orderRepository.save(any())).thenAnswer(
                invocation -> {
                    Order order = invocation.getArgument(0);
                    ReflectionTestUtils.setField(order, "id", id);
                    return order;
                }
        );
    }

}
