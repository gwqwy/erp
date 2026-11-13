package cn.tiangong.module.hr.dal.dataobject;

import cn.tiangong.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 出差申请（走审批流 biz_type=hr_trip）。
 */

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hr_trip")
public class HrTripDO extends BaseDO {

    private String no;

    private Long employeeId;

    private LocalDate startDate;

    private LocalDate endDate;

    private String destination;

    private String reason;

    private Integer status;

}
