package com.fooddelivery.notificationservice.entity;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Document(collection = "notifications")
public class Notification {

    @Id
    private String id;           // MongoDB ObjectId — stored as String

    @Indexed
    private String username;     // indexed for fast per-user queries

    private String type;         // "ORDER_CONFIRMED" or "ORDER_CANCELLED"
    private String message;      // human-readable text sent to the user
    private Long orderId;
    private Long paymentId;
    private BigDecimal amount;
    private boolean read;        // false until user marks it read

    @CreatedDate
    private LocalDateTime createdAt;

    public Notification() {}

    public Notification(String username, String type, String message,
                        Long orderId, Long paymentId, BigDecimal amount) {
        this.username = username;
        this.type = type;
        this.message = message;
        this.orderId = orderId;
        this.paymentId = paymentId;
        this.amount = amount;
        this.read = false;
    }

    public String getId() { return id; }
    public String getUsername() { return username; }
    public String getType() { return type; }
    public String getMessage() { return message; }
    public Long getOrderId() { return orderId; }
    public Long getPaymentId() { return paymentId; }
    public BigDecimal getAmount() { return amount; }
    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}