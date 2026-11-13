package cn.tiangong.module.file.service;

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
    private final cn.tiangong.module.file.dal.mysql.ErpFileMapper fileMapper;
    private final FileStorageService fileStorageService;

    private static final String CHUNK_DIR = System.getProperty("user.home") + "/tiangong-files/chunk/";

    /** 初始化：命中 MD5 → 秒传；有同 uploadId 会话 → 断点续传（返回已收分片）；否则新建会话 */
    public InitResult init(String fileName, String md5, Integer totalChunks, Long totalSize) {
        // 秒传：erp_file 已有同 MD5 记录（本体已在正式存储，前端直接完成）
        cn.tiangong.module.file.dal.dataobject.ErpFileDO hit = fileMapper.selectByMd5(md5);
        if (hit != null) {
            return new InitResult("instant", hit.getFileName(), hit.getFileSize(), null);
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
