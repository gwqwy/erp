package cn.tiangong.module.file.dal.dataobject;

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
