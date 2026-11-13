package cn.tiangong.module.im.websocket;

import jakarta.websocket.Session;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 在线用户会话表：userId → WebSocket Session。
 */
@Slf4j
public final class ImSessionManager {

    private static final Map<Long, Session> ONLINE = new ConcurrentHashMap<>();

    private ImSessionManager() {
    }

    public static void put(Long userId, Session session) {
        Session old = ONLINE.put(userId, session);
        if (old != null && old != session) {
            try { old.close(); } catch (Exception ignore) { }
        }
    }

    public static void remove(Long userId, Session session) {
        ONLINE.remove(userId, session);
    }

    public static Session get(Long userId) {
        return ONLINE.get(userId);
    }

    public static Map<Long, Session> snapshot() {
        return Map.copyOf(ONLINE);
    }
}
