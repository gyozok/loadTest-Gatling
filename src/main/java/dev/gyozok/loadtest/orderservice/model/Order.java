package dev.gyozok.loadtest.orderservice.model;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name="orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    String item;

    @Column(nullable = false)
    int quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    OrderStatus status;

    @Column(nullable = false, updatable = false)
    Instant createdAt;

    public Order() {
    }

    public Order(String item, int quantity, OrderStatus status, Instant createdAt) {
        this.item = item;
        this.quantity = quantity;
        this.status = status;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getItem() {
        return item;
    }

    public int getQuantity() {
        return quantity;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }
}
