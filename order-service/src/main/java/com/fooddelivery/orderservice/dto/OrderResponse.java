package com.fooddelivery.orderservice.dto;

import com.fooddelivery.orderservice.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Long id,
        String username,
        OrderStatus status,
        BigDecimal totalAmount,
        String shippingAddress,
        List<OrderItemResponse> items,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}