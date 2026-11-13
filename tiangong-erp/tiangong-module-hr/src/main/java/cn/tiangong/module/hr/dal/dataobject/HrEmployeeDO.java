package cn.tiangong.module.hr.dal.dataobject;

import cn.tiangong.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 员工档案（精简版）。部门引用 system_dept；status：0 在职 / 1 离职。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hr_employee")
public class HrEmployeeDO extends BaseDO {

    private String no;

    private String name;

    private Long deptId;

    private String post;

    /** 月基本工资（算薪用） */
    private BigDecimal baseSalary;

    private String phone;

    private String email;

    private LocalDate entryDate;

    private LocalDate leaveDate;

    private Integer status;
}
