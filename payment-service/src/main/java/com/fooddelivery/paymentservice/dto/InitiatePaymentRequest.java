package com.fooddelivery.paymentservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record InitiatePaymentRequest(
        @NotNull Long orderId,
        @NotNull @DecimalMin("0.01") BigDecimal amount,
        @NotBlank String username
) {}