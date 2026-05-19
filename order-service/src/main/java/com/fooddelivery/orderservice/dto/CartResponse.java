package com.fooddelivery.orderservice.dto;

import java.math.BigDecimal;
import java.util.List;

public record CartResponse(
        String username,
        List<CartItemResponse> items,
        BigDecimal total
) {}