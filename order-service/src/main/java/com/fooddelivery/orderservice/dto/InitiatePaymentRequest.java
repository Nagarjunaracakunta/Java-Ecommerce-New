package com.fooddelivery.orderservice.dto;

import java.math.BigDecimal;

public record InitiatePaymentRequest(
        Long orderId,
        BigDecimal amount,
        String username
) {}