package cn.tiangong.framework.biz.common.chatter.dal.dataobject;

import cn.tiangong.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Chatter 记录动态（借鉴 Odoo）：biz_type + biz_id 多态关联任意单据。
 * msg_type：comment 评论 / system_trace 系统轨迹（审核等关键动作的字段快照）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_chatter")
public class BizChatterDO extends BaseDO {

    private String bizType;

    private Long bizId;

    private String msgType;

    private String content;

    private Long parentId;

    /** 发布人 userId，system_trace 为空 */
    private Long userId;

    /** system_trace 的附加数据，如 {"status":{"old":10,"new":30}} */
    private String extra;
}
