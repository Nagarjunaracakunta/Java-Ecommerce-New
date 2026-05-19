package com.fooddelivery.notificationservice.repository;

import com.fooddelivery.notificationservice.entity.Notification;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface NotificationRepository extends MongoRepository<Notification, String> {

    List<Notification> findByUsernameOrderByCreatedAtDesc(String username);

    List<Notification> findByUsernameAndReadFalseOrderByCreatedAtDesc(String username);

    long countByUsernameAndReadFalse(String username);
}