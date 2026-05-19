package com.fooddelivery.orderservice.client;

import com.fooddelivery.orderservice.dto.InitiatePaymentRequest;
import com.fooddelivery.orderservice.util.ServiceTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class PaymentClient {

    private static final Logger log = LoggerFactory.getLogger(PaymentClient.class);

    private final RestClient restClient;
    private final ServiceTokenProvider serviceTokenProvider;

    @Value("${payment.service.url}")
    private String paymentServiceUrl;

    public PaymentClient(RestClient restClient, ServiceTokenProvider serviceTokenProvider) {
        this.restClient = restClient;
        this.serviceTokenProvider = serviceTokenProvider;
    }

    public void initiatePayment(InitiatePaymentRequest request) {
        try {
            restClient.post()
                    .uri(paymentServiceUrl + "/payments")
                    .header("Authorization", "Bearer " + serviceTokenProvider.token())
                    .header("Content-Type", "application/json")
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception ex) {
            // Payment initiation failure is non-fatal — order stays PENDING.
            // The Kafka consumer will update status once payment-service responds.
            log.error("Failed to initiate payment for order {}: {}", request.orderId(), ex.getMessage());
        }
    }
}