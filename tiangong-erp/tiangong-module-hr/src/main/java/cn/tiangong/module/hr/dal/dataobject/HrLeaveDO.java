package cn.tiangong.module.hr.dal.dataobject;

import cn.tiangong.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 请假申请（走轻量审批流：提交 → 逐级审批 → 通过/驳回）。
 * status：10 草稿 20 审批中 30 已通过 50 已驳回 60 已作废。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hr_leave")
public class HrLeaveDO extends BaseDO {

    private String no;

    private Long employeeId;

    /** 事假 / 年假 / 病假 / 调休 */
    private String leaveType;

    private LocalDate startDate;

    private LocalDate endDate;

    private String reason;

    private Integer status;

    private String remark;
}
