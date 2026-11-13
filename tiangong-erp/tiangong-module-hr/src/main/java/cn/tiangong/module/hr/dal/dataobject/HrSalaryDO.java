package cn.tiangong.module.hr.dal.dataobject;

import cn.tiangong.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 月度薪资（手工登记，汇总页）。实发 = 应发 - 扣款。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hr_salary")
public class HrSalaryDO extends BaseDO {

    /** 会计期间 yyyy-MM */
    private String period;

    private Long employeeId;

    private BigDecimal grossAmount;

    private BigDecimal deduction;

    private BigDecimal netAmount;

    private String remark;
}
