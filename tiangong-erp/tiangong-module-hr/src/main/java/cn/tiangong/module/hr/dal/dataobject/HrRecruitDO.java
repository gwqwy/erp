package cn.tiangong.module.hr.dal.dataobject;

import cn.tiangong.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hr_recruit")
public class HrRecruitDO extends BaseDO {

    private String no;

    private String position;

    private Integer headcount;

    private String department;

    private Integer status;

    private String remark;
}
