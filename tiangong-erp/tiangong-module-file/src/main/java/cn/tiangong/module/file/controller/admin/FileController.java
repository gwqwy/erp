package cn.tiangong.module.file.controller.admin;

import cn.tiangong.framework.security.core.LoginUser;
import cn.tiangong.framework.security.util.SecurityFrameworkUtils;
import cn.tiangong.framework.web.core.pojo.CommonResult;
import cn.tiangong.module.file.dal.dataobject.ErpFileDO;
import cn.tiangong.module.file.dal.mysql.ErpFileMapper;
import cn.tiangong.module.file.service.FileChunkService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.dromara.x.file.storage.core.FileStorageService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;

/** 文件中心：普通上传 / 分片上传（秒传+断点续传）/ 下载 / 列表。 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/admin-api/file")
public class FileController {

    private final FileStorageService fileStorageService;
    private final FileChunkService chunkService;
    private final ErpFileMapper fileMapper;

    // ========== 普通上传 ==========

    @PostMapping("/upload")
    @PreAuthorize("@ss.hasPermission('file:upload')")
    public CommonResult<ErpFileDO> upload(@RequestParam("file") MultipartFile file,
                                          @RequestParam(value = "bizType", required = false) String bizType,
                                          @RequestParam(value = "bizId", required = false) Long bizId) throws Exception {
        LoginUser user = SecurityFrameworkUtils.getLoginUser();
        // 入正式存储
        var detail = fileStorageService.of(file)
                .setPath("storage/")
                .upload();
        ErpFileDO record = new ErpFileDO();
        record.setFileName(file.getOriginalFilename());
        record.setFileKey(detail.getPath() + detail.getFilename());
        record.setMd5(md5Of(file));
        record.setFileSize(file.getSize());
        record.setContentType(file.getContentType());
        record.setUrl(detail.getUrl());
        record.setBizType(bizType == null ? "" : bizType);
        record.setBizId(bizId);
        record.setUploaderId(user != null ? user.getId() : 0L);
        record.setUploaderName(user != null ? user.getUsername() : "");
        fileMapper.insert(record);
        return CommonResult.success(record);
    }

    // ========== 分片上传（Free-FS 协议：init / chunk / merge） ==========

    /** 初始化：秒传检测 + 断点续传检测 + 新会话 */
    @PostMapping("/chunk/init")
    @PreAuthorize("@ss.hasPermission('file:upload')")
    public CommonResult<Object> chunkInit(@RequestParam("fileName") String fileName,
                                          @RequestParam("md5") String md5,
                                          @RequestParam(value = "totalChunks", required = false) Integer totalChunks,
                                          @RequestParam(value = "totalSize", required = false) Long totalSize) throws Exception {
        return CommonResult.success(chunkService.init(fileName, md5, totalChunks, totalSize));
    }

    /** 逐片上传 */
    @PostMapping("/chunk/upload")
    @PreAuthorize("@ss.hasPermission('file:upload')")
    public CommonResult<Boolean> chunkUpload(@RequestParam("uploadId") String uploadId,
                                             @RequestParam("chunkIndex") Integer chunkIndex,
                                             @RequestParam("file") MultipartFile file) throws Exception {
        chunkService.saveChunk(uploadId, chunkIndex, file);
        return CommonResult.success(true);
    }

    /** 合并分片：合并后通过 x-file-storage 入正式存储并登记 */
    @PostMapping("/chunk/merge")
    @PreAuthorize("@ss.hasPermission('file:upload')")
    public CommonResult<ErpFileDO> chunkMerge(@RequestParam("uploadId") String uploadId,
                                              @RequestParam("fileName") String fileName,
                                              @RequestParam("md5") String md5,
                                              @RequestParam("totalChunks") Integer totalChunks) throws Exception {
        LoginUser user = SecurityFrameworkUtils.getLoginUser();
        FileChunkService.MergedFile merged = chunkService.merge(uploadId, totalChunks);
        var detail = fileStorageService.of(merged.file())
                .setPath("storage/")
                .setName(md5 + "_" + fileName)
                .upload();

        ErpFileDO record = new ErpFileDO();
        record.setFileName(fileName);
        record.setFileKey(detail.getPath() + detail.getFilename());
        record.setMd5(md5);
        record.setFileSize(merged.size());
        record.setContentType(contentTypeOf(fileName));
        record.setUrl(detail.getUrl());
        record.setUploaderId(user != null ? user.getId() : 0L);
        record.setUploaderName(user != null ? user.getUsername() : "");
        fileMapper.insert(record);
        return CommonResult.success(record);
    }

    /** 秒传检测：md5 命中即返回已有文件（前端直接完成上传） */
    @GetMapping("/check-md5")
    @PreAuthorize("@ss.hasPermission('file:upload')")
    public CommonResult<ErpFileDO> checkMd5(@RequestParam("md5") String md5) {
        return CommonResult.success(fileMapper.selectByMd5(md5));
    }

    // ========== 下载 / 列表 / 删除 ==========

    @GetMapping("/download")
    @PreAuthorize("@ss.hasPermission('file:download')")
    public void download(@RequestParam("id") Long id, HttpServletResponse response) throws IOException {
        ErpFileDO record = fileMapper.selectById(id);
        if (record == null) {
            response.setStatus(404);
            return;
        }
        File local = new java.io.File(System.getProperty("user.home") + "/tiangong-files/" + record.getFileKey());
        if (!local.exists()) {
            // 兼容 x-file-storage URL 抽取的相对路径
            local = new java.io.File(System.getProperty("user.home") + "/tiangong-files/storage/" + record.getFileName());
        }
        response.setContentType(record.getContentType() == null || record.getContentType().isBlank()
                ? "application/octet-stream" : record.getContentType());
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''"
                + java.net.URLEncoder.encode(record.getFileName(), "UTF-8").replace("+", "%20"));
        java.nio.file.Files.copy(local.toPath(), response.getOutputStream());
    }

    @GetMapping("/list")
    @PreAuthorize("@ss.hasPermission('file:list')")
    public CommonResult<List<ErpFileDO>> getList(
            @RequestParam(required = false) String fileName,
            @RequestParam(required = false) String bizType,
            @RequestParam(required = false) Long bizId) {
        return CommonResult.success(fileMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ErpFileDO>()
                        .like(fileName != null && !fileName.isBlank(), ErpFileDO::getFileName, fileName)
                        .eq(bizType != null && !bizType.isBlank(), ErpFileDO::getBizType, bizType)
                        .eq(bizId != null, ErpFileDO::getBizId, bizId)
                        .orderByDesc(ErpFileDO::getId)));
    }

    @DeleteMapping("/delete")
    @PreAuthorize("@ss.hasPermission('file:delete')")
    public CommonResult<Boolean> delete(@RequestParam("id") Long id) {
        fileMapper.deleteById(id);
        return CommonResult.success(true);
    }

    private String md5Of(MultipartFile file) throws IOException {
        try (var in = file.getInputStream()) {
            var digest = java.security.MessageDigest.getInstance("MD5");
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) {
                digest.update(buf, 0, n);
            }
            var hex = new StringBuilder();
            for (byte b : digest.digest()) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            // MD5 计算异常时用时间戳兜底（不参与秒传）
            return String.valueOf(System.currentTimeMillis());
        }
    }

    private String contentTypeOf(String fileName) {
        String lower = fileName == null ? "" : fileName.toLowerCase();
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".pdf")) return "application/pdf";
        if (lower.endsWith(".txt")) return "text/plain";
        return "application/octet-stream";
    }
}
