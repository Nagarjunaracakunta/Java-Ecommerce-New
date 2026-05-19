package com.fooddelivery.cartservice.controller;

import com.fooddelivery.cartservice.dto.CartItemRequest;
import com.fooddelivery.cartservice.dto.CartResponse;
import com.fooddelivery.cartservice.service.CartService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping("/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public ResponseEntity<CartResponse> getCart(Principal principal) {
        return ResponseEntity.ok(cartService.getCart(principal.getName()));
    }

    @PostMapping("/items")
    public ResponseEntity<Void> addItem(Principal principal,
                                        @Valid @RequestBody CartItemRequest request) {
        cartService.addItem(principal.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/items/{productId}")
    public ResponseEntity<Void> removeItem(Principal principal,
                                           @PathVariable Long productId) {
        cartService.removeItem(principal.getName(), productId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> clearCart(Principal principal) {
        cartService.clearCart(principal.getName());
        return ResponseEntity.noContent().build();
    }

    // ── Internal endpoints — accessible only via service token (ROLE_SERVICE) ──
    // Not routed through the API gateway, so only reachable within the Docker network.

    @PreAuthorize("hasRole('SERVICE')")
    @GetMapping("/internal/{username}")
    public ResponseEntity<CartResponse> getCartInternal(@PathVariable String username) {
        return ResponseEntity.ok(cartService.getCart(username));
    }

    @PreAuthorize("hasRole('SERVICE')")
    @DeleteMapping("/internal/{username}")
    public ResponseEntity<Void> clearCartInternal(@PathVariable String username) {
        cartService.clearCart(username);
        return ResponseEntity.noContent().build();
    }
}