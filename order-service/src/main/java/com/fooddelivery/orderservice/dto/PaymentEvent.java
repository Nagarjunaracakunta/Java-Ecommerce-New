package com.fooddelivery.orderservice.dto;

import java.math.BigDecimal;

public record PaymentEvent(
        String eventType,
        Long paymentId,
        Long orderId,
        String username,
        BigDecimal amount,
        String failureReason
) {}