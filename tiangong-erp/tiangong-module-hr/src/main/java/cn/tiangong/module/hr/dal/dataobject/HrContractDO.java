package cn.tiangong.module.hr.dal.dataobject;

import cn.tiangong.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 劳动合同。status：0 履行中 1 已到期 2 已解除。
 */

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hr_contract")
public class HrContractDO extends BaseDO {

    private String no;

    private Long employeeId;

    private LocalDate startDate;

    private LocalDate endDate;

    private Integer status;

    private String remark;

}
