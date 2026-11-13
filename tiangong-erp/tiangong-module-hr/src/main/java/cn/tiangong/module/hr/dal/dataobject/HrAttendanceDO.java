package cn.tiangong.module.hr.dal.dataobject;

import cn.tiangong.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 考勤记录：每日一条（employee + date 唯一）。status：0 正常 1 迟到 2 早退 3 缺勤 4 事假 5 年假。
 */

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hr_attendance")
public class HrAttendanceDO extends BaseDO {

    private Long employeeId;

    private LocalDate attDate;

    private LocalTime checkIn;

    private LocalTime checkOut;

    private Integer status;

    private String remark;

}
