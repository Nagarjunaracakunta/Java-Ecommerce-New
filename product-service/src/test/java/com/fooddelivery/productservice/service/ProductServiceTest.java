package com.fooddelivery.productservice.service;

import com.fooddelivery.productservice.dto.ProductRequest;
import com.fooddelivery.productservice.dto.ProductResponse;
import com.fooddelivery.productservice.entity.Category;
import com.fooddelivery.productservice.entity.Product;
import com.fooddelivery.productservice.exception.ProductNotFoundException;
import com.fooddelivery.productservice.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    private Product sampleProduct() {
        return new Product("Burger", "Classic beef burger", new BigDecimal("9.99"), 50, Category.MAIN_COURSE);
    }

    private ProductRequest sampleRequest() {
        return new ProductRequest("Burger", "Classic beef burger", new BigDecimal("9.99"), 50, Category.MAIN_COURSE);
    }

    // --- getAllProducts ---

    @Test
    void getAllProducts_shouldReturnAllWhenNoCategoryFilter() {
        when(productRepository.findAll()).thenReturn(List.of(sampleProduct()));

        List<ProductResponse> result = productService.getAllProducts(null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Burger");
        verify(productRepository).findAll();
        verify(productRepository, never()).findByCategory(any());
    }

    @Test
    void getAllProducts_shouldFilterByCategoryWhenProvided() {
        when(productRepository.findByCategory(Category.MAIN_COURSE)).thenReturn(List.of(sampleProduct()));

        List<ProductResponse> result = productService.getAllProducts(Category.MAIN_COURSE);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).category()).isEqualTo(Category.MAIN_COURSE);
        verify(productRepository).findByCategory(Category.MAIN_COURSE);
        verify(productRepository, never()).findAll();
    }

    // --- getProductById ---

    @Test
    void getProductById_shouldReturnProductWhenFound() {
        Product product = sampleProduct();
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        ProductResponse result = productService.getProductById(1L);

        assertThat(result.name()).isEqualTo("Burger");
        assertThat(result.price()).isEqualByComparingTo("9.99");
    }

    @Test
    void getProductById_shouldThrowWhenNotFound() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProductById(99L))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessageContaining("99");
    }

    // --- createProduct ---

    @Test
    void createProduct_shouldSaveAndReturnProduct() {
        Product saved = sampleProduct();
        when(productRepository.save(any(Product.class))).thenReturn(saved);

        ProductResponse result = productService.createProduct(sampleRequest());

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("Burger");
        assertThat(captor.getValue().getCategory()).isEqualTo(Category.MAIN_COURSE);
        assertThat(result.name()).isEqualTo("Burger");
    }

    // --- updateProduct ---

    @Test
    void updateProduct_shouldUpdateAndReturnProduct() {
        Product existing = sampleProduct();
        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(productRepository.save(existing)).thenReturn(existing);

        ProductRequest updateRequest = new ProductRequest("Cheeseburger", "With extra cheese",
                new BigDecimal("11.99"), 30, Category.MAIN_COURSE);

        ProductResponse result = productService.updateProduct(1L, updateRequest);

        assertThat(existing.getName()).isEqualTo("Cheeseburger");
        assertThat(existing.getPrice()).isEqualByComparingTo("11.99");
        assertThat(result.name()).isEqualTo("Cheeseburger");
    }

    @Test
    void updateProduct_shouldThrowWhenNotFound() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.updateProduct(99L, sampleRequest()))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessageContaining("99");
    }

    // --- deleteProduct ---

    @Test
    void deleteProduct_shouldDeleteWhenExists() {
        when(productRepository.existsById(1L)).thenReturn(true);

        productService.deleteProduct(1L);

        verify(productRepository).deleteById(1L);
    }

    @Test
    void deleteProduct_shouldThrowWhenNotFound() {
        when(productRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> productService.deleteProduct(99L))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessageContaining("99");

        verify(productRepository, never()).deleteById(any());
    }
}