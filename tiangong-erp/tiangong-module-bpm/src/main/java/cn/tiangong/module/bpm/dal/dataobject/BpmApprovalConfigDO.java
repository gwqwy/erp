package cn.tiangong.module.bpm.dal.dataobject;

import cn.tiangong.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 审批流配置：单据类型 × 审批级次 → 审批人。按 level 从小到大逐级审批。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("bpm_approval_config")
public class BpmApprovalConfigDO extends BaseDO {

    /** 业务类型：pur_order / pur_inbound / sal_order / sal_outbound / hr_leave */
    private String bizType;

    /** 审批级次，1 起始 */
    private Integer level;

    /** 该级次审批人 */
    private Long approverUserId;

    private Integer enabled;
}
