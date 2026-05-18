package com.fooddelivery.productservice.dto;

import com.fooddelivery.productservice.entity.Category;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ProductRequest(
        @NotBlank @Size(max = 255) String name,
        @Size(max = 1000) String description,
        @NotNull @DecimalMin("0.01") @DecimalMax("99999.99") BigDecimal price,
        @Min(0) @Max(100000) int stock,
        @NotNull Category category
) {}