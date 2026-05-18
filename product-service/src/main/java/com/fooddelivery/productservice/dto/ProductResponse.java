package com.fooddelivery.productservice.dto;

import com.fooddelivery.productservice.entity.Category;
import com.fooddelivery.productservice.entity.Product;

import java.math.BigDecimal;

public record ProductResponse(
        Long id,
        String name,
        String description,
        BigDecimal price,
        int stock,
        Category category
) {
    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getStock(),
                product.getCategory()
        );
    }
}