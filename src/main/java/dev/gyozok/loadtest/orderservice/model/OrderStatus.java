package dev.gyozok.loadtest.orderservice.model;

/**
 * Lifecycle states for an order.
 *
 * PENDING   - order accepted and persisted, OrderCreated event published to Kafka,
 *             downstream processing not yet confirmed.
 * CONFIRMED - downstream processing completed successfully.
 * FAILED    - downstream processing failed (e.g. inventory check failed).
 */
public enum OrderStatus {
    PENDING,
    CONFIRMED,
    FAILED
}
