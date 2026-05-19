package com.fooddelivery.orderservice.service;

import com.fooddelivery.orderservice.client.CartClient;
import com.fooddelivery.orderservice.client.PaymentClient;
import com.fooddelivery.orderservice.dto.CartResponse;
import com.fooddelivery.orderservice.dto.InitiatePaymentRequest;
import com.fooddelivery.orderservice.dto.CreateOrderRequest;
import com.fooddelivery.orderservice.dto.OrderItemResponse;
import com.fooddelivery.orderservice.dto.OrderResponse;
import com.fooddelivery.orderservice.entity.Order;
import com.fooddelivery.orderservice.entity.OrderItem;
import com.fooddelivery.orderservice.entity.OrderStatus;
import com.fooddelivery.orderservice.exception.OrderNotFoundException;
import com.fooddelivery.orderservice.repository.OrderRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartClient cartClient;
    private final PaymentClient paymentClient;

    public OrderService(OrderRepository orderRepository, CartClient cartClient,
                        PaymentClient paymentClient) {
        this.orderRepository = orderRepository;
        this.cartClient = cartClient;
        this.paymentClient = paymentClient;
    }

    @Transactional
    public OrderResponse createOrder(String username, CreateOrderRequest request) {
        CartResponse cart = cartClient.getCart(username);

        if (cart.items() == null || cart.items().isEmpty()) {
            throw new IllegalStateException("Cannot place an order with an empty cart");
        }

        Order order = new Order(username, cart.total(), request.shippingAddress());

        cart.items().forEach(item -> {
            OrderItem orderItem = new OrderItem(
                    order,
                    item.productId(),
                    item.productName(),
                    item.price(),
                    item.quantity(),
                    item.subtotal()
            );
            order.getItems().add(orderItem);
        });

        Order saved = orderRepository.save(order);

        // Clear cart only after order is persisted successfully
        cartClient.clearCart(username);

        // Trigger payment asynchronously — order stays PENDING until Kafka event arrives
        paymentClient.initiatePayment(
                new InitiatePaymentRequest(saved.getId(), saved.getTotalAmount(), username));

        return toResponse(saved);
    }

    public List<OrderResponse> getMyOrders(String username) {
        return orderRepository.findByUsernameOrderByCreatedAtDesc(username)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public OrderResponse getOrder(String username, Long orderId, boolean isAdmin) {
        if (isAdmin) {
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));
            return toResponse(order);
        }

        Order order = orderRepository.findByIdAndUsername(orderId, username)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));
        return toResponse(order);
    }

    @Transactional
    public OrderResponse cancelOrder(String username, Long orderId, boolean isAdmin) {
        Order order;

        if (isAdmin) {
            order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));
        } else {
            order = orderRepository.findByIdAndUsername(orderId, username)
                    .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));
        }

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException(
                    "Only PENDING orders can be cancelled. Current status: " + order.getStatus());
        }

        order.setStatus(OrderStatus.CANCELLED);
        return toResponse(orderRepository.save(order));
    }

    private OrderResponse toResponse(Order order) {
        List<OrderItemResponse> items = order.getItems().stream()
                .map(i -> new OrderItemResponse(
                        i.getProductId(), i.getProductName(),
                        i.getPrice(), i.getQuantity(), i.getSubtotal()))
                .toList();

        return new OrderResponse(
                order.getId(), order.getUsername(), order.getStatus(),
                order.getTotalAmount(), order.getShippingAddress(),
                items, order.getCreatedAt(), order.getUpdatedAt());
    }
}