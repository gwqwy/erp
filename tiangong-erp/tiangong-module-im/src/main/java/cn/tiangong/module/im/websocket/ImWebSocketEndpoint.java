package cn.tiangong.module.im.websocket;

import cn.tiangong.framework.security.core.LoginUser;
import cn.tiangong.framework.security.core.TokenService;
import cn.tiangong.module.im.config.WsBeanHolder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * IM WebSocket 端点：/ws/im/{token}。
 * 协议（参考 box-im 简化）：
 * - 客户端心跳："ping" → 回 "pong"
 * - 客户端聊天：{"type":"chat","receiverId":2,"content":"hi"} → 服务端落库并推送在线接收方
 * - 服务端确认/推送：{"type":"chat",...} / {"type":"open"}
 * 端点实例由 WS 容器创建，依赖通过 WsBeanHolder 取（Boot 无 root context）。
 */
@Slf4j
@Component
@ServerEndpoint(value = "/ws/im/{token}")
public class ImWebSocketEndpoint {

    private Session session;
    private Long userId;

    @OnOpen
    public void onOpen(Session session, @PathParam("token") String token) {
        this.session = session;
        try {
            TokenService tokenService = WsBeanHolder.get(TokenService.class);
            LoginUser loginUser = tokenService.checkToken(token);
            if (loginUser == null) {
                log.warn("[onOpen] token 无效，关闭连接");
                try { session.close(); } catch (Exception ignore) { }
                return;
            }
            this.userId = loginUser.getId();
            ImSessionManager.put(this.userId, session);
            send(this.session, "{\"type\":\"open\"}");
        } catch (Exception e) {
            log.error("[onOpen] 处理失败: {}", e.getMessage(), e);
        }
    }

    @OnMessage
    public void onMessage(String text, Session session) {
        if ("ping".equals(text.trim())) {
            send(session, "pong");
            return;
        }
        try {
            ObjectMapper objectMapper = WsBeanHolder.get(ObjectMapper.class);
            ImPushService imPushService = WsBeanHolder.get(ImPushService.class);
            JsonNode node = objectMapper.readTree(text);
            String type = node.path("type").asText("chat");
            if ("chat".equals(type)) {
                Long receiver = node.path("receiverId").asLong();
                String content = node.path("content").asText();
                imPushService.pushChat(this.userId, receiver, content);
            }
        } catch (Exception e) {
            log.warn("[onMessage] 处理失败: {}", e.getMessage());
        }
    }

    @OnClose
    public void onClose(Session session) {
        // userId 在连接建立时记录；无法拿到实例字段时按 session 反查
        for (var e : ImSessionManager.snapshot().entrySet()) {
            if (e.getValue() == session) {
                ImSessionManager.remove(e.getKey(), session);
                break;
            }
        }
    }

    @OnError
    public void onError(Session session, Throwable t) {
        log.warn("[onError] {}", t.getMessage());
    }

    private void send(Session to, String text) {
        try {
            to.getBasicRemote().sendText(text);
        } catch (Exception ignore) {
        }
    }
}
