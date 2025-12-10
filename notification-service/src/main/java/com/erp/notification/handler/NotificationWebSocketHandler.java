package com.erp.notification.handler;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class NotificationWebSocketHandler extends TextWebSocketHandler {

    // 사용자 ID별 세션 저장소 (Key: userId, Value: WebSocketSession)
    private static final Map<String, WebSocketSession> userSessions = new ConcurrentHashMap<>();

    // 외부(Controller)에서 호출하여 알림 전송
    public void sendNotification(String userId, String message) {
        WebSocketSession session = userSessions.get(userId);
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(message));
                System.out.println("Sent to " + userId + ": " + message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("User " + userId + " is not connected.");
        }
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // 접속 URL 예시: ws://localhost:8084/ws?id=1
        String query = session.getUri().getQuery();
        String userId = getUserIdFromQuery(query);

        if (userId != null) {
            userSessions.put(userId, session);
            System.out.println("User connected: " + userId);
            session.sendMessage(new TextMessage("Connected successfully!"));
        } else {
            session.close(CloseStatus.BAD_DATA);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        // 연결 끊기면 세션 제거 (메모리 누수 방지)
        userSessions.values().remove(session);
        System.out.println("User disconnected.");
    }

    // 쿼리 스트링 파싱 (?id=1 -> "1")
    private String getUserIdFromQuery(String query) {
        if (query != null && query.contains("id=")) {
            for (String param : query.split("&")) {
                String[] pair = param.split("=");
                if (pair.length == 2 && "id".equals(pair[0])) {
                    return pair[1];
                }
            }
        }
        return null;
    }
}