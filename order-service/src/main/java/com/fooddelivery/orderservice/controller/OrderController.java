package com.fooddelivery.orderservice.controller;

import com.fooddelivery.orderservice.dto.CreateOrderRequest;
import com.fooddelivery.orderservice.dto.OrderResponse;
import com.fooddelivery.orderservice.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(Authentication auth,
                                                     @Valid @RequestBody CreateOrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(orderService.createOrder(auth.getName(), request));
    }

    @GetMapping
    public ResponseEntity<List<OrderResponse>> getMyOrders(Authentication auth) {
        return ResponseEntity.ok(orderService.getMyOrders(auth.getName()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrder(Authentication auth, @PathVariable Long id) {
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        return ResponseEntity.ok(orderService.getOrder(auth.getName(), id, isAdmin));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<OrderResponse> cancelOrder(Authentication auth, @PathVariable Long id) {
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        return ResponseEntity.ok(orderService.cancelOrder(auth.getName(), id, isAdmin));
    }
}