package com.fooddelivery.productservice.service;

import com.fooddelivery.productservice.dto.ProductRequest;
import com.fooddelivery.productservice.dto.ProductResponse;
import com.fooddelivery.productservice.entity.Category;
import com.fooddelivery.productservice.entity.Product;
import com.fooddelivery.productservice.exception.ProductNotFoundException;
import com.fooddelivery.productservice.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<ProductResponse> getAllProducts(Category category) {
        List<Product> products = category != null
                ? productRepository.findByCategory(category)
                : productRepository.findAll();
        return products.stream().map(ProductResponse::from).toList();
    }

    public ProductResponse getProductById(Long id) {
        return ProductResponse.from(
                productRepository.findById(id)
                        .orElseThrow(() -> new ProductNotFoundException("Product not found: " + id))
        );
    }

    public ProductResponse createProduct(ProductRequest request) {
        Product product = new Product(
                request.name(),
                request.description(),
                request.price(),
                request.stock(),
                request.category()
        );
        return ProductResponse.from(productRepository.save(product));
    }

    public ProductResponse updateProduct(Long id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Product not found: " + id));
        product.setName(request.name());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setStock(request.stock());
        product.setCategory(request.category());
        return ProductResponse.from(productRepository.save(product));
    }

    public void deleteProduct(Long id) {
        if (!productRepository.existsById(id)) {
            throw new ProductNotFoundException("Product not found: " + id);
        }
        productRepository.deleteById(id);
    }
}