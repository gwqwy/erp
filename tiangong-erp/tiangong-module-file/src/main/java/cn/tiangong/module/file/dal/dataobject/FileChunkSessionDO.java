package cn.tiangong.module.file.dal.dataobject;

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
