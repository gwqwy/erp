package cn.tiangong.module.hr.dal.dataobject;

import cn.tiangong.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hr_performance")
public class HrPerformanceDO extends BaseDO {

    private String no;

    private Long employeeId;

    private String period;

    private BigDecimal score;

    private String grade;

    private String remark;
}
