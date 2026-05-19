package com.fooddelivery.paymentservice.service;

import com.fooddelivery.paymentservice.dto.InitiatePaymentRequest;
import com.fooddelivery.paymentservice.dto.PaymentEvent;
import com.fooddelivery.paymentservice.dto.PaymentResponse;
import com.fooddelivery.paymentservice.entity.Payment;
import com.fooddelivery.paymentservice.entity.PaymentStatus;
import com.fooddelivery.paymentservice.exception.PaymentNotFoundException;
import com.fooddelivery.paymentservice.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);
    private static final String TOPIC = "payment-events";

    private final PaymentRepository paymentRepository;
    private final KafkaTemplate<String, PaymentEvent> kafkaTemplate;

    public PaymentService(PaymentRepository paymentRepository,
                          KafkaTemplate<String, PaymentEvent> kafkaTemplate) {
        this.paymentRepository = paymentRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Transactional
    public PaymentResponse initiatePayment(InitiatePaymentRequest request) {
        Payment payment = new Payment(request.orderId(), request.username(), request.amount());
        paymentRepository.save(payment);

        try {
            // Simulate payment processing — always succeeds.
            // Replace this block with a real payment gateway call (Stripe, PayPal, etc.)
            process(payment);

            payment.setStatus(PaymentStatus.SUCCESS);
            paymentRepository.save(payment);

            publish(new PaymentEvent(
                    "PAYMENT_SUCCESS", payment.getId(), payment.getOrderId(),
                    payment.getUsername(), payment.getAmount(), null));

            log.info("Payment {} succeeded for order {}", payment.getId(), payment.getOrderId());

        } catch (Exception ex) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason(ex.getMessage());
            paymentRepository.save(payment);

            publish(new PaymentEvent(
                    "PAYMENT_FAILED", payment.getId(), payment.getOrderId(),
                    payment.getUsername(), payment.getAmount(), ex.getMessage()));

            log.warn("Payment {} failed for order {}: {}", payment.getId(), payment.getOrderId(), ex.getMessage());
        }

        return toResponse(payment);
    }

    public PaymentResponse getById(Long id) {
        return toResponse(paymentRepository.findById(id)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found: " + id)));
    }

    public PaymentResponse getByOrderId(Long orderId) {
        return toResponse(paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new PaymentNotFoundException("No payment found for order: " + orderId)));
    }

    private void process(Payment payment) {
        // Stub — always succeeds. Throw RuntimeException here to simulate failure.
        log.debug("Processing payment {} for order {} amount {}",
                payment.getId(), payment.getOrderId(), payment.getAmount());
    }

    private void publish(PaymentEvent event) {
        kafkaTemplate.send(TOPIC, String.valueOf(event.orderId()), event);
        log.debug("Published {} to topic {}", event.eventType(), TOPIC);
    }

    private PaymentResponse toResponse(Payment p) {
        return new PaymentResponse(p.getId(), p.getOrderId(), p.getUsername(),
                p.getAmount(), p.getStatus(), p.getFailureReason(),
                p.getCreatedAt(), p.getUpdatedAt());
    }
}