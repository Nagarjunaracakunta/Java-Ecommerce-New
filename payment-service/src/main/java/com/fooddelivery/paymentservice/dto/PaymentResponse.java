package com.fooddelivery.paymentservice.dto;

import com.fooddelivery.paymentservice.entity.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentResponse(
        Long id,
        Long orderId,
        String username,
        BigDecimal amount,
        PaymentStatus status,
        String failureReason,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}