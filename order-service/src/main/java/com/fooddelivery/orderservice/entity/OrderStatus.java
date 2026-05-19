package com.fooddelivery.orderservice.entity;

public enum OrderStatus {
    PENDING,      // created, awaiting payment
    CONFIRMED,    // payment succeeded (set by payment-service via Kafka)
    SHIPPED,      // dispatched
    DELIVERED,    // completed
    CANCELLED     // cancelled by user or payment failure
}