package cn.tiangong.module.hr.dal.dataobject;

import cn.tiangong.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 离职申请（走审批流 biz_type=hr_resign；通过后自动改员工状态为离职）。
 */

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hr_resignation")
public class HrResignationDO extends BaseDO {

    private String no;

    private Long employeeId;

    private LocalDate applyDate;

    private LocalDate planLeaveDate;

    private String reason;

    private Integer status;

}
