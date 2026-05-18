package com.fooddelivery.productservice.repository;

import com.fooddelivery.productservice.entity.Category;
import com.fooddelivery.productservice.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByCategory(Category category);

    List<Product> findByNameContainingIgnoreCase(String name);
}