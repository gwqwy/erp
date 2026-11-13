package cn.tiangong.module.infra.dal.dataobject;

import cn.tiangong.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 公告通知。status：0 发布 / 1 撤回。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("infra_notice")
public class InfraNoticeDO extends BaseDO {

    private String title;

    private String content;

    private String publishUser;

    private LocalDateTime publishTime;

    private Integer status;
}
