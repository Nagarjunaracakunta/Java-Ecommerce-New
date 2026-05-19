package com.fooddelivery.orderservice.client;

import com.fooddelivery.orderservice.dto.CartResponse;
import com.fooddelivery.orderservice.util.ServiceTokenProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class CartClient {

    private final RestClient restClient;
    private final ServiceTokenProvider serviceTokenProvider;

    @Value("${cart.service.url}")
    private String cartServiceUrl;

    public CartClient(RestClient restClient, ServiceTokenProvider serviceTokenProvider) {
        this.restClient = restClient;
        this.serviceTokenProvider = serviceTokenProvider;
    }

    public CartResponse getCart(String username) {
        return restClient.get()
                .uri(cartServiceUrl + "/cart/internal/" + username)
                .header("Authorization", "Bearer " + serviceTokenProvider.token())
                .retrieve()
                .body(CartResponse.class);
    }

    public void clearCart(String username) {
        restClient.delete()
                .uri(cartServiceUrl + "/cart/internal/" + username)
                .header("Authorization", "Bearer " + serviceTokenProvider.token())
                .retrieve()
                .toBodilessEntity();
    }
}