package com.fooddelivery.cartservice.service;

import com.fooddelivery.cartservice.dto.CartItemRequest;
import com.fooddelivery.cartservice.dto.CartItemResponse;
import com.fooddelivery.cartservice.dto.CartResponse;
import com.fooddelivery.cartservice.dto.ProductResponse;
import com.fooddelivery.cartservice.exception.ProductNotFoundException;
import com.fooddelivery.cartservice.util.ServiceTokenProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class CartService {

    private static final String CART_KEY_PREFIX = "cart:";

    private final StringRedisTemplate redis;
    private final RestClient restClient;
    private final ServiceTokenProvider serviceTokenProvider;

    @Value("${product.service.url}")
    private String productServiceUrl;

    public CartService(StringRedisTemplate redis, RestClient restClient,
                       ServiceTokenProvider serviceTokenProvider) {
        this.redis = redis;
        this.restClient = restClient;
        this.serviceTokenProvider = serviceTokenProvider;
    }

    public void addItem(String username, CartItemRequest request) {
        // Validate product exists before touching the cart
        fetchProduct(request.productId());

        String key = cartKey(username);
        String field = String.valueOf(request.productId());

        String existing = (String) redis.opsForHash().get(key, field);
        int newQty = (existing != null ? Integer.parseInt(existing) : 0) + request.quantity();
        redis.opsForHash().put(key, field, String.valueOf(newQty));
    }

    public CartResponse getCart(String username) {
        Map<Object, Object> entries = redis.opsForHash().entries(cartKey(username));

        List<CartItemResponse> items = new ArrayList<>();
        for (Map.Entry<Object, Object> entry : entries.entrySet()) {
            Long productId = Long.parseLong((String) entry.getKey());
            int quantity   = Integer.parseInt((String) entry.getValue());

            ProductResponse product = fetchProduct(productId);
            BigDecimal subtotal = product.price().multiply(BigDecimal.valueOf(quantity));

            items.add(new CartItemResponse(productId, product.name(), product.price(), quantity, subtotal));
        }

        BigDecimal total = items.stream()
                .map(CartItemResponse::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CartResponse(username, items, total);
    }

    public void removeItem(String username, Long productId) {
        long removed = redis.opsForHash().delete(cartKey(username), String.valueOf(productId));
        if (removed == 0) {
            throw new ProductNotFoundException("Product " + productId + " is not in the cart");
        }
    }

    public void clearCart(String username) {
        redis.delete(cartKey(username));
    }

    private ProductResponse fetchProduct(Long productId) {
        return restClient.get()
                .uri(productServiceUrl + "/products/" + productId)
                .header("Authorization", "Bearer " + serviceTokenProvider.token())
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                    throw new ProductNotFoundException("Product not found: " + productId);
                })
                .body(ProductResponse.class);
    }

    private String cartKey(String username) {
        return CART_KEY_PREFIX + username;
    }
}