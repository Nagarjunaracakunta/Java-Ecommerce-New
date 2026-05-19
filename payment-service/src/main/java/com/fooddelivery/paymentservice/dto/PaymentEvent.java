package com.fooddelivery.paymentservice.dto;

import java.math.BigDecimal;

public record PaymentEvent(
        String eventType,       // PAYMENT_SUCCESS or PAYMENT_FAILED
        Long paymentId,
        Long orderId,
        String username,
        BigDecimal amount,
        String failureReason    // null on success
) {}