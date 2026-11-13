package cn.tiangong.module.system.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日志（AOP 自动记录写操作）。不做逻辑删除，历史只读。
 */
@Data
@TableName("system_oper_log")
public class SystemOperLogDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String username;

    private String url;

    private String httpMethod;

    private String paramsText;

    private Long costMs;

    private Boolean success;

    private String errorMsg;

    private LocalDateTime createTime;
}
