package com.fooddelivery.notificationservice.service;

import com.fooddelivery.notificationservice.dto.PaymentEvent;
import com.fooddelivery.notificationservice.entity.Notification;
import com.fooddelivery.notificationservice.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public void handlePaymentEvent(PaymentEvent event) {
        String type;
        String message;

        switch (event.eventType()) {
            case "PAYMENT_SUCCESS" -> {
                type = "ORDER_CONFIRMED";
                message = String.format(
                        "Your order #%d has been confirmed! Payment of $%s was successful.",
                        event.orderId(), event.amount());
            }
            case "PAYMENT_FAILED" -> {
                type = "ORDER_CANCELLED";
                message = String.format(
                        "Payment failed for order #%d. Your order has been cancelled. Reason: %s",
                        event.orderId(),
                        event.failureReason() != null ? event.failureReason() : "Unknown error");
            }
            default -> {
                log.warn("Unknown payment event type: {}", event.eventType());
                return;
            }
        }

        Notification notification = new Notification(
                event.username(), type, message,
                event.orderId(), event.paymentId(), event.amount());

        notificationRepository.save(notification);
        log.info("Saved notification [{}] for user '{}' — order {}", type, event.username(), event.orderId());
    }

    public List<Notification> getUnread(String username) {
        return notificationRepository.findByUsernameAndReadFalseOrderByCreatedAtDesc(username);
    }

    public List<Notification> getAll(String username) {
        return notificationRepository.findByUsernameOrderByCreatedAtDesc(username);
    }

    public long countUnread(String username) {
        return notificationRepository.countByUsernameAndReadFalse(username);
    }

    public void markRead(String username, String notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found: " + notificationId));

        if (!notification.getUsername().equals(username)) {
            throw new IllegalArgumentException("Notification does not belong to this user");
        }

        notification.setRead(true);
        notificationRepository.save(notification);
    }

    public void markAllRead(String username) {
        List<Notification> unread =
                notificationRepository.findByUsernameAndReadFalseOrderByCreatedAtDesc(username);
        unread.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(unread);
    }
}