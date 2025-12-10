package com.erp.notification.controller;

import com.erp.notification.handler.NotificationWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationWebSocketHandler webSocketHandler;

    // Request Service가 호출할 API
    @PostMapping("/notify")
    public void sendNotification(@RequestBody Map<String, String> payload) {
        String userId = payload.get("userId");
        String message = payload.get("message");

        if (userId != null && message != null) {
            webSocketHandler.sendNotification(userId, message);
        }
    }
}