package com.fooddelivery.orderservice.dto;

import java.math.BigDecimal;

public record OrderItemResponse(
        Long productId,
        String productName,
        BigDecimal price,
        int quantity,
        BigDecimal subtotal
) {}