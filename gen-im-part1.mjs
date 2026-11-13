// V12 后端 Java 代码生成：DO / Mapper / Config / WebSocket
import fs from 'node:fs';
import path from 'node:path';

const pkgRoot = (mod) => `E:/文件/编程文件/erp/tiangong-erp/tiangong-module-${mod}/src/main/java/cn/tiangong/module/${mod}`;

function write(mod, rel, content) {
  const fp = path.join(pkgRoot(mod), rel);
  fs.mkdirSync(path.dirname(fp), { recursive: true });
  fs.writeFileSync(fp, content, 'utf8');
  console.log('write:', mod + '/' + rel.replace('dal/mysql/', '').slice(0, 60));
}

// ============ FILE 模块 ============
write('file', 'dal/dataobject/ErpFileDO.java', `package cn.tiangong.module.file.dal.dataobject;

import cn.tiangong.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 文件记录：上传成功后登记；md5 用于秒传判断。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("erp_file")
public class ErpFileDO extends BaseDO {

    private String fileName;

    private String fileKey;

    private String md5;

    private Long fileSize;

    private String contentType;

    private String url;

    private String bizType;

    private Long bizId;

    private Long uploaderId;

    private String uploaderName;
}
`);

write('file', 'dal/mysql/ErpFileMapper.java', `package cn.tiangong.module.file.dal.mysql;

import cn.tiangong.framework.mybatis.core.mapper.BaseMapperX;
import cn.tiangong.module.file.dal.dataobject.ErpFileDO;
import org.apache.ibatis.annotations.Select;

public interface ErpFileMapper extends BaseMapperX<ErpFileDO> {

    @Select("SELECT * FROM erp_file WHERE md5 = #{md5} AND deleted = 0 LIMIT 1")
    ErpFileDO selectByMd5(String md5);
}
`);

write('file', 'dal/dataobject/FileChunkSessionDO.java', `package cn.tiangong.module.file.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 分片上传会话（非 BaseDO：临时数据，不参与业务审计）。
 */
@Data
@TableName("file_chunk_session")
public class FileChunkSessionDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String uploadId;

    private String fileName;

    private String md5;

    private Integer totalChunks;

    private Long totalSize;

    /** 已收分片索引 JSON 数组，如 [0,1,3] */
    private String received;

    private LocalDateTime createTime;
}
`);

write('file', 'dal/mysql/FileChunkSessionMapper.java', `package cn.tiangong.module.file.dal.mysql;

import cn.tiangong.framework.mybatis.core.mapper.BaseMapperX;
import cn.tiangong.module.file.dal.dataobject.FileChunkSessionDO;

public interface FileChunkSessionMapper extends BaseMapperX<FileChunkSessionDO> {
}
`);

// x-file-storage 配置（编程式，避免 yml 嵌套键在多环境丢失）
write('file', 'config/FileStorageConfig.java', `package cn.tiangong.module.file.config;

import org.dromara.x.file.storage.core.FileStorageProperties;
import org.dromara.x.file.storage.core.FileStorageService;
import org.dromara.x.file.storage.spring.SpringFileStorageProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * x-file-storage 配置：本地磁盘存储（生产可加 MinIO/OSS 平台节点切换）。
 * 存储根目录 {user.home}/tiangong-files/，正式文件在 storage/，分片临时在 chunk/。
 */
@Configuration
@Import(SpringFileStorageProperties.class)
@EnableConfigurationProperties(FileStorageProperties.class)
public class FileStorageConfig {

    @Bean
    public FileStorageService fileStorageService(SpringFileStorageProperties properties) {
        // 默认平台 local-plus-1；如 yml 有配置则以 yml 为准（此处仅兜底）
        if (properties.getPlatform().stream().noneMatch(p -> "local-plus-1".equals(p.getPlatform()))) {
            FileStorageProperties.LocalPlus local = new FileStorageProperties.LocalPlus();
            local.setPlatform("local-plus-1");
            local.setEnableStorage(true);
            local.setBasePath(System.getProperty("user.home") + "/tiangong-files/");
            local.setStoragePath("/storage/");
            properties.getLocalPlus().add(local);
        }
        return new FileStorageService(properties);
    }
}
`);

write('file', 'config/FileStorageYmlInitializer.java', `package cn.tiangong.module.file.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.Environment;

import java.util.HashMap;
import java.util.Map;

/**
 * 将 x-file-storage 配置以最高优先级注入环境（等价于 yml 的 dromara.x-file-storage 节点），
 * 避免 yml 嵌套键在多环境覆盖问题。
 */
public class FileStorageYmlInitializer implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Map<String, Object> map = new HashMap<>();
        map.put("dromara.x-file-storage.default-platform", "local-plus-1");
        map.put("dromara.x-file-storage.enable-storage", "true");
        map.put("dromara.x-file-storage.local-plus[0].platform", "local-plus-1");
        map.put("dromara.x-file-storage.local-plus[0].enable-storage", "true");
        map.put("dromara.x-file-storage.local-plus[0].base-path",
                System.getProperty("user.home") + "/tiangong-files/");
        map.put("dromara.x-file-storage.local-plus[0].storage-path", "/storage/");
        map.put("dromara.x-file-storage.enable-file-recorder", "false");
        environment.getPropertySources().addFirst(new MapPropertySource("tiangongFileStorage", map));
    }
}
`);

// META-INF 注册 EnvironmentPostProcessor（Spring Boot 3 用独立文件）
write('file', '../../resources/META-INF/spring.factories', `org.springframework.boot.env.EnvironmentPostProcessor=\\
cn.tiangong.module.file.config.FileStorageYmlInitializer
`);

write('file', 'service/FileChunkService.java', `package cn.tiangong.module.file.service;

import cn.hutool.core.io.file.FileWriterUtil;
import cn.tiangong.module.file.dal.dataobject.FileChunkSessionDO;
import cn.tiangong.module.file.dal.mysql.FileChunkSessionMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.dromara.x.file.storage.core.FileStorageService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 分片上传：Free-FS 协议思路 —— init(秒传/断点检测) → chunk(逐片) → merge(合并入库)。
 */
@Service
@RequiredArgsConstructor
public class FileChunkService {

    private final FileChunkSessionMapper sessionMapper;
    private final FileStorageService fileStorageService;

    private static final String CHUNK_DIR = System.getProperty("user.home") + "/tiangong-files/chunk/";

    /** 初始化：命中 MD5 → 秒传；有同 uploadId 会话 → 断点续传（返回已收分片）；否则新建会话 */
    public InitResult init(String fileName, String md5, Integer totalChunks, Long totalSize) {
        // 秒传：同 MD5 完整文件已存在
        File existing = findMergedFile(md5);
        if (existing != null) {
            return new InitResult("instant", existing.getName(), existing.length(), null);
        }
        // 断点续传：找未过期会话（2 小时内）
        FileChunkSessionDO session = sessionMapper.selectOne(new LambdaQueryWrapper<FileChunkSessionDO>()
                .eq(FileChunkSessionDO::getMd5, md5)
                .eq(FileChunkSessionDO::getFileName, fileName)
                .ge(FileChunkSessionDO::getCreateTime, LocalDateTime.now().minusHours(2))
                .last("LIMIT 1"));
        if (session != null) {
            List<Integer> received = parseReceived(session.getReceived());
            return new InitResult("resume", session.getUploadId(), null, received);
        }
        session = new FileChunkSessionDO();
        session.setUploadId(java.util.UUID.randomUUID().toString().replace("-", ""));
        session.setFileName(fileName);
        session.setMd5(md5);
        session.setTotalChunks(totalChunks == null ? 0 : totalChunks);
        session.setTotalSize(totalSize == null ? 0 : totalSize);
        session.setReceived("[]");
        session.setCreateTime(LocalDateTime.now());
        sessionMapper.insert(session);
        return new InitResult("new", session.getUploadId(), null, List.of());
    }

    /** 接收一个分片，写入临时目录 */
    public void saveChunk(String uploadId, Integer chunkIndex, MultipartFile file) throws IOException {
        Path dir = Path.of(CHUNK_DIR + uploadId);
        Files.createDirectories(dir);
        file.transferTo(dir.resolve("chunk_" + chunkIndex).toFile());
        FileChunkSessionDO session = sessionMapper.selectOne(new LambdaQueryWrapper<FileChunkSessionDO>()
                .eq(FileChunkSessionDO::getUploadId, uploadId).last("LIMIT 1"));
        if (session != null) {
            List<Integer> received = parseReceived(session.getReceived());
            if (!received.contains(chunkIndex)) {
                received.add(chunkIndex);
                session.setReceived(toJson(received));
                sessionMapper.updateById(session);
            }
        }
    }

    /** 合并分片 → 通过 x-file-storage 入正式存储；返回合并文件（调用方负责建 erp_file 记录） */
    public MergedFile merge(String uploadId, Integer totalChunks) throws IOException {
        Path dir = Path.of(CHUNK_DIR + uploadId);
        File target = dir.resolve("merged").toFile();
        try (var out = Files.newOutputStream(target.toPath())) {
            for (int i = 0; i < totalChunks; i++) {
                Path chunk = dir.resolve("chunk_" + i);
                if (!Files.exists(chunk)) {
                    throw new IllegalStateException("分片缺失: " + i);
                }
                Files.copy(chunk, out);
            }
        }
        File merged = target;
        return new MergedFile(merged.getName(), merged.length(), merged);
    }

    public record MergedFile(String fileName, long size, File file) {
    }

    private File findMergedFile(String md5) {
        // 秒传：正式存储中按 md5 命名查找
        File storageDir = Path.of(System.getProperty("user.home") + "/tiangong-files/storage/").toFile();
        File[] hits = storageDir.listFiles((d, n) -> n.startsWith(md5));
        return hits != null && hits.length > 0 ? hits[0] : null;
    }

    private List<Integer> parseReceived(String json) {
        try {
            return new ObjectMapper().readValue(json == null ? "[]" : json,
                    new com.fasterxml.jackson.core.type.TypeReference<List<Integer>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private String toJson(List<Integer> list) {
        try {
            return new ObjectMapper().writeValueAsString(list);
        } catch (Exception e) {
            return "[]";
        }
    }

    public record InitResult(String mode, String uploadIdOrName, Long size, List<Integer> received) {
    }
}
`);

// IM 模块
write('im', 'config/WebSocketConfig.java', `package cn.tiangong.module.im.config;

import cn.tiangong.module.im.websocket.ImWebSocketEndpoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

/**
 * WebSocket 配置：注册 @ServerEndpoint 端点（外置 Tomcat 必须显式注册 Exporter）。
 */
@Configuration
public class WebSocketConfig {

    @Bean
    public ServerEndpointExporter serverEndpointExporter() {
        return new ServerEndpointExporter();
    }
}
`);

write('im', 'websocket/ImSessionManager.java', `package cn.tiangong.module.im.websocket;

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

    public static boolean online(Long userId) {
        Session s = ONLINE.get(userId);
        return s != null && s.isOpen();
    }
}
`);

write('im', 'websocket/ImWebSocketEndpoint.java', `package cn.tiangong.module.im.websocket;

import cn.tiangong.framework.security.core.TokenService;
import cn.tiangong.framework.security.core.LoginUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * IM WebSocket 端点：/ws/im/{token}。
 * 消息协议（参考 box-im 简化）：{"type":"chat","receiverId":2,"content":"hi"}
 * 服务端确认：{"type":"ack"}；离线消息走 REST 拉取（message 落库即不丢）。
 */
@Slf4j
@Component
@ServerEndpoint(value = "/ws/im/{token}", configurator = SpringConfiguratorForIm.class)
public class ImWebSocketEndpoint {

    private Session session;
    private Long userId;

    private static ImPushService pushService;

    /** Spring 注入静态字段：@ServerEndpoint 每连接新建实例，需经 Configurator */
    @Autowired
    public void setPushService(ImPushService service) {
        ImWebSocketEndpoint.pushService = service;
    }

    @OnOpen
    public void onOpen(Session session, @PathParam("token") String token) {
        this.session = session;
        try {
            var claims = tokenServiceInstance().parseToken(token);
            this.userId = claims.userId();
            ImSessionManager.put(userId, session);
            send(session, "{\\"type\\":\\"open\\"}");
        } catch (Exception e) {
            log.warn("[onOpen] token 校验失败: {}", e.getMessage());
            try { session.close(); } catch (Exception ignore) { }
        }
    }

    @OnMessage
    public void onMessage(String text, Session session) {
        // 心跳
        if ("\\"ping\\"".equals(text)) {
            send(session, "\\"pong\\"");
            return;
        }
        try {
            Map<?, ?> msg = new com.fasterxml.jackson.databind.ObjectMapper().readValue(text, Map.class);
            Long receiver = Long.valueOf(String.valueOf(msg.get("receiverId")));
            String content = String.valueOf(msg.get("content"));
            if (pushService != null) {
                pushService.sendChat(userId, receiver, content);
            }
        } catch (Exception e) {
            log.warn("[onMessage] 消息处理失败: {}", e.getMessage());
        }
    }

    @OnClose
    public void onClose(Session session) {
        if (userId != null) {
            ImSessionManager.remove(userId, session);
        }
    }

    static TokenServiceHolder holder;

    private TokenService tokenServiceInstance() {
        return ImPushServiceHolder.tokenService;
    }

    public interface TokenServiceHolder {
    }

    private void send(Session to, String text) {
        try { to.getBasicRemote().sendText(text); } catch (Exception ignore) { }
    }
}
`);

console.log('part1 done');
