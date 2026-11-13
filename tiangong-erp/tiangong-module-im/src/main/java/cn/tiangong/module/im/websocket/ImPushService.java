package cn.tiangong.module.im.websocket;

import cn.tiangong.module.im.dal.dataobject.ImMessageDO;
import cn.tiangong.module.im.dal.dataobject.ImSessionDO;
import cn.tiangong.module.im.dal.mysql.ImMessageMapper;
import cn.tiangong.module.im.dal.mysql.ImSessionMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * IM 推送：落库 im_message + 更新 im_session + 在线则 WebSocket 推送接收方。
 */
@Slf4j
@Service
public class ImPushService {

    private final ImMessageMapper messageMapper;
    private final ImSessionMapper sessionMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ImPushService(ImMessageMapper messageMapper, ImSessionMapper sessionMapper) {
        this.messageMapper = messageMapper;
        this.sessionMapper = sessionMapper;
    }

    /** REST 发送入口：senderId 为当前登录人 */
    public ImMessageDO sendChat(Long senderId, Long receiverId, String content) {
        return doSend(senderId, receiverId, content);
    }

    /** WebSocket 聊天入口 */
    public void pushChat(Long senderId, Long receiverId, String content) {
        doSend(senderId, receiverId, content);
    }

    public ImMessageDO doSend(Long senderId, Long receiverId, String content) {
        String sessionKey = sessionKey(senderId, receiverId);
        LocalDateTime now = LocalDateTime.now();

        ImMessageDO msg = new ImMessageDO();
        msg.setSessionId(sessionKey);
        msg.setSenderId(senderId);
        msg.setReceiverId(receiverId);
        msg.setContent(content);
        msg.setMsgType("text");
        msg.setReadFlag(false);
        msg.setCreateTime(now);
        messageMapper.insert(msg);

        ImSessionDO session = sessionMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ImSessionDO>()
                        .eq(ImSessionDO::getSessionKey, sessionKey));
        if (session == null) {
            session = new ImSessionDO();
            session.setSessionKey(sessionKey);
            session.setUserA(senderId.compareTo(receiverId) < 0 ? senderId : receiverId);
            session.setUserB(senderId.compareTo(receiverId) < 0 ? receiverId : senderId);
            session.setLastContent(truncate(content));
            session.setLastTime(now);
            sessionMapper.insert(session);
        } else {
            session.setLastContent(truncate(content));
            session.setLastTime(now);
            sessionMapper.updateById(session);
        }

        // 在线推送
        var receiverSession = cn.tiangong.module.im.websocket.ImSessionManager.get(receiverId);
        if (receiverSession != null && receiverSession.isOpen()) {
            try {
                Map<String, Object> payload = new HashMap<>();
                payload.put("type", "chat");
                payload.put("sessionId", sessionKey);
                payload.put("senderId", senderId);
                payload.put("content", content);
                payload.put("createTime", now.toString());
                receiverSession.getBasicRemote().sendText(objectMapper.writeValueAsString(payload));
            } catch (Exception e) {
                log.warn("[doSend] 推送失败（接收方可能掉线，消息已落库）: {}", e.getMessage());
            }
        }
        return msg;
    }

    private String sessionKey(Long a, Long b) {
        return a.compareTo(b) < 0 ? a + "_" + b : b + "_" + a;
    }

    private String truncate(String s) {
        return s != null && s.length() > 200 ? s.substring(0, 200) : s;
    }
}
