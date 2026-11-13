package cn.tiangong.module.hr.dal.dataobject;

import cn.tiangong.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hr_training")
public class HrTrainingDO extends BaseDO {

    private String no;

    private String title;

    private String trainer;

    private LocalDate trainDate;

    private String employees;

    private String remark;
}
