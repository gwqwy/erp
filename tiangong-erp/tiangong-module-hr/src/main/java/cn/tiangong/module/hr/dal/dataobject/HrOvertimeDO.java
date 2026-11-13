package cn.tiangong.module.hr.dal.dataobject;

import cn.tiangong.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 加班申请（走审批流 biz_type=hr_overtime）。
 */

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hr_overtime")
public class HrOvertimeDO extends BaseDO {

    private String no;

    private Long employeeId;

    private LocalDate otDate;

    private BigDecimal hours;

    private String reason;

    private Integer status;

}
