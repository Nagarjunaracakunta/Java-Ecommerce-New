package com.fooddelivery.notificationservice.kafka;

import com.fooddelivery.notificationservice.dto.PaymentEvent;
import com.fooddelivery.notificationservice.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentEventListener {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventListener.class);

    private final NotificationService notificationService;

    public PaymentEventListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @KafkaListener(topics = "payment-events", groupId = "notification-service")
    public void onPaymentEvent(PaymentEvent event) {
        log.info("Received payment event: {} for order {} user '{}'",
                event.eventType(), event.orderId(), event.username());
        notificationService.handlePaymentEvent(event);
    }
}