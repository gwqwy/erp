package cn.tiangong.module.bpm.dal.dataobject;

import cn.tiangong.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 审批任务实例：单据提交审批时按配置生成全部级次任务，逐级通过。
 * status：1 待审批 / 2 已通过 / 3 已驳回 / 4 已撤回。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("bpm_approval_task")
public class BpmApprovalTaskDO extends BaseDO {

    private String bizType;

    private Long bizId;

    private String bizNo;

    private Integer level;

    private Long approverUserId;

    private Integer status;

    private String comment;
}
