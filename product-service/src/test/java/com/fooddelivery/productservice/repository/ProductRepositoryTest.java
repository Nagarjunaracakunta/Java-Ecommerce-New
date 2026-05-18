package com.fooddelivery.productservice.repository;

import com.fooddelivery.productservice.entity.Category;
import com.fooddelivery.productservice.entity.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    @BeforeEach
    void setUp() {
        productRepository.save(new Product("Burger", "Classic beef", new BigDecimal("9.99"), 50, Category.MAIN_COURSE));
        productRepository.save(new Product("Cola", "Cold drink", new BigDecimal("2.49"), 100, Category.BEVERAGES));
        productRepository.save(new Product("Fries", "Crispy fries", new BigDecimal("3.99"), 80, Category.SIDES));
    }

    @Test
    void findByCategory_shouldReturnOnlyMatchingCategory() {
        List<Product> result = productRepository.findByCategory(Category.BEVERAGES);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Cola");
    }

    @Test
    void findByCategory_shouldReturnEmptyWhenNoMatch() {
        List<Product> result = productRepository.findByCategory(Category.DESSERT);
        assertThat(result).isEmpty();
    }

    @Test
    void findByNameContainingIgnoreCase_shouldMatchCaseInsensitive() {
        List<Product> result = productRepository.findByNameContainingIgnoreCase("burger");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Burger");
    }

    @Test
    void findByNameContainingIgnoreCase_shouldReturnEmptyWhenNoMatch() {
        List<Product> result = productRepository.findByNameContainingIgnoreCase("pizza");
        assertThat(result).isEmpty();
    }

    @Test
    void findAll_shouldReturnAllSavedProducts() {
        List<Product> all = productRepository.findAll();
        assertThat(all).hasSize(3);
    }
}