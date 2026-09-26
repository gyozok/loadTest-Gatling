package dev.gyozok.loadtest.orderconsumer.event;

import java.time.Instant;

/**
 * payload to publish to the Kafka topic "order-events" when the order is created.
 */
public record OrderCreatedEvent(
        Long orderId,
        String item,
        int quantity,
        Instant createdAt
) {
}
