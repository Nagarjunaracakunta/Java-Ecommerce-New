package com.fooddelivery.orderservice.kafka;

import com.fooddelivery.orderservice.dto.PaymentEvent;
import com.fooddelivery.orderservice.entity.Order;
import com.fooddelivery.orderservice.entity.OrderStatus;
import com.fooddelivery.orderservice.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class PaymentEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventConsumer.class);

    private final OrderRepository orderRepository;

    public PaymentEventConsumer(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @KafkaListener(topics = "payment-events", groupId = "order-service")
    @Transactional
    public void onPaymentEvent(PaymentEvent event) {
        log.info("Received payment event: {} for order {}", event.eventType(), event.orderId());

        Order order = orderRepository.findById(event.orderId()).orElse(null);
        if (order == null) {
            log.warn("Order {} not found for payment event — skipping", event.orderId());
            return;
        }

        switch (event.eventType()) {
            case "PAYMENT_SUCCESS" -> {
                order.setStatus(OrderStatus.CONFIRMED);
                log.info("Order {} confirmed after successful payment {}", order.getId(), event.paymentId());
            }
            case "PAYMENT_FAILED" -> {
                order.setStatus(OrderStatus.CANCELLED);
                log.info("Order {} cancelled after failed payment {}: {}",
                        order.getId(), event.paymentId(), event.failureReason());
            }
            default -> log.warn("Unknown payment event type: {}", event.eventType());
        }

        orderRepository.save(order);
    }
}