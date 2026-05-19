package com.fooddelivery.notificationservice.controller;

import com.fooddelivery.notificationservice.entity.Notification;
import com.fooddelivery.notificationservice.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ResponseEntity<List<Notification>> getUnread(Principal principal) {
        return ResponseEntity.ok(notificationService.getUnread(principal.getName()));
    }

    @GetMapping("/all")
    public ResponseEntity<List<Notification>> getAll(Principal principal) {
        return ResponseEntity.ok(notificationService.getAll(principal.getName()));
    }

    @GetMapping("/count")
    public ResponseEntity<Map<String, Long>> countUnread(Principal principal) {
        long count = notificationService.countUnread(principal.getName());
        return ResponseEntity.ok(Map.of("unread", count));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markRead(Principal principal, @PathVariable String id) {
        notificationService.markRead(principal.getName(), id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllRead(Principal principal) {
        notificationService.markAllRead(principal.getName());
        return ResponseEntity.noContent().build();
    }
}