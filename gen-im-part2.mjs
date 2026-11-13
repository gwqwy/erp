import fs from 'node:fs';
import path from 'node:path';

const root = 'E:/文件/编程文件/erp/tiangong-erp/tiangong-module-im/src/main/java/cn/tiangong/module/im';

function write(rel, content) {
  const fp = path.join(root, rel);
  fs.mkdirSync(path.dirname(fp), { recursive: true });
  fs.writeFileSync(fp, content, 'utf8');
  console.log('write:', rel);
}

// 干净版 WebSocket 端点（覆盖 part1 的混乱版）
write('websocket/ImWebSocketEndpoint.java', `package cn.tiangong.module.im.websocket;

import cn.tiangong.framework.security.core.LoginUser;
import cn.tiangong.framework.security.core.TokenService;
import cn.tiangong.module.im.service.ImPushService;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.server.standard.SpringConfigurator;

/**
 * IM WebSocket 端点：/ws/im/{token}。
 * 协议（参考 box-im 简化）：
 * - 客户端心跳："ping" → 回 "pong"
 * - 客户端聊天：{"type":"chat","receiverId":2,"content":"hi"} → 服务端落库并推送在线接收方
 * - 服务端确认/推送：{"type":"chat","senderId":...,"content":...} / {"type":"open"}
 * 离线消息不推送（上线走 REST 拉未读）。
 */
@Slf4j
@Component
@ServerEndpoint(value = "/ws/im/{token}", configurator = SpringConfigurator.class)
public class ImWebSocketEndpoint {

    private Session session;
    private Long userId;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private ImPushService imPushService;

    @Autowired
    private ObjectMapper objectMapper;

    @OnOpen
    public void onOpen(Session session, @PathParam("token") String token) {
        this.session = session;
        LoginUser loginUser = tokenService.checkToken(token);
        if (loginUser == null) {
            log.warn("[onOpen] token 无效，关闭连接");
            try { session.close(); } catch (Exception ignore) { }
            return;
        }
        this.userId = loginUser.getId();
        ImSessionManager.put(this.userId, session);
        send(this.session, "{\\"type\\":\\"open\\"}");
    }

    @OnMessage
    public void onMessage(String text, Session session) {
        if ("\\"ping\\"".equals(text.trim())) {
            send(session, "\\"pong\\"");
            return;
        }
        try {
            JsonNode node = objectMapper.readTree(text);
            String type = node.path("type").asText("chat");
            if ("chat".equals(type) && imPushService != null) {
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
`);

write('websocket/ImSessionManager.java', `package cn.tiangong.module.im.websocket;

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
`);

write('websocket/ImPushService.java', `package cn.tiangong.module.im.websocket;

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
`);

write('dal/dataobject/ImMessageDO.java', `package cn.tiangong.module.im.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * IM 单聊消息。
 */
@Data
@TableName("im_message")
public class ImMessageDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String sessionId;

    private Long senderId;

    private Long receiverId;

    private String content;

    private String msgType;

    private Boolean readFlag;

    private LocalDateTime createTime;
}
`);

write('dal/dataobject/ImSessionDO.java', `package cn.tiangong.module.im.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * IM 会话（冗余最后一条消息，会话列表用）。
 */
@Data
@TableName("im_session")
public class ImSessionDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String sessionKey;

    private Long userA;

    private Long userB;

    private String lastContent;

    private LocalDateTime lastTime;

    private LocalDateTime updateTime;
}
`);

write('dal/mysql/ImMessageMapper.java', `package cn.tiangong.module.im.dal.mysql;

import cn.tiangong.framework.mybatis.core.mapper.BaseMapperX;
import cn.tiangong.module.im.dal.dataobject.ImMessageDO;

public interface ImMessageMapper extends BaseMapperX<ImMessageDO> {
}
`);

write('dal/mysql/ImSessionMapper.java', `package cn.tiangong.module.im.dal.mysql;

import cn.tiangong.framework.mybatis.core.mapper.BaseMapperX;
import cn.tiangong.module.im.dal.dataobject.ImSessionDO;

public interface ImSessionMapper extends BaseMapperX<ImSessionDO> {
}
`);

write('service/ImChatService.java', `package cn.tiangong.module.im.service;

import cn.tiangong.module.im.dal.dataobject.ImMessageDO;
import cn.tiangong.module.im.dal.dataobject.ImSessionDO;
import cn.tiangong.module.im.dal.mysql.ImMessageMapper;
import cn.tiangong.module.im.dal.mysql.ImSessionMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * IM 查询服务：会话列表、历史消息。
 */
@RequiredArgsConstructor
@Service
public class ImChatService {

    private final ImMessageMapper messageMapper;
    private final ImSessionMapper sessionMapper;

    /** 我的会话列表（含对方 id 与最后一条消息） */
    public List<ImSessionDO> mySessions(Long userId) {
        return sessionMapper.selectList(new LambdaQueryWrapper<ImSessionDO>()
                .eq(ImSessionDO::getUserA, userId)
                .or()
                .eq(ImSessionDO::getUserB, userId)
                .orderByDesc(ImSessionDO::getLastTime));
    }

    /** 与某人的历史消息（升序） */
    public List<ImMessageDO> history(String sessionKey) {
        return messageMapper.selectList(new LambdaQueryWrapper<ImMessageDO>()
                .eq(ImMessageDO::getSessionId, sessionKey)
                .orderByAsc(ImMessageDO::getId));
    }
}
`);

write('controller/ImController.java', `package cn.tiangong.module.im.controller.admin;

import cn.tiangong.framework.security.core.LoginUser;
import cn.tiangong.framework.security.core.TokenService;
import cn.tiangong.framework.web.core.pojo.CommonResult;
import cn.tiangong.module.im.dal.dataobject.ImMessageDO;
import cn.tiangong.module.im.dal.dataobject.ImSessionDO;
import cn.tiangong.module.im.service.ImChatService;
import cn.tiangong.module.im.websocket.ImPushService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/admin-api/im")
public class ImController {

    private final ImChatService chatService;
    private final ImPushService pushService;
    private final TokenService tokenService;

    @GetMapping("/sessions")
    @PreAuthorize("@ss.hasPermission('im:query')")
    public CommonResult<List<ImSessionDO>> mySessions(
            @RequestParam(value = "token", required = false) String token) {
        Long me = currentUserId();
        return CommonResult.success(chatService.mySessions(me));
    }

    @GetMapping("/history")
    @PreAuthorize("@ss.hasPermission('im:query')")
    public CommonResult<List<ImMessageDO>> history(@RequestParam("sessionKey") String sessionKey) {
        return CommonResult.success(chatService.history(sessionKey));
    }

    /** REST 发送（与 WebSocket 发送等效，消息落库 + 在线推送） */
    @PostMapping("/send")
    @PreAuthorize("@ss.hasPermission('im:chat')")
    public CommonResult<ImMessageDO> send(@RequestParam("receiverId") @NotNull Long receiverId,
                                          @RequestParam("content") @NotBlank String content) {
        Long me = currentUserId();
        return CommonResult.success(pushService.sendChat(me, receiverId, content));
    }

    private Long currentUserId() {
        LoginUser user = cn.tiangong.framework.security.util.SecurityFrameworkUtils.getLoginUser();
        return user != null ? user.getId() : null;
    }
}
`);

console.log('part2 done');
