package cn.tiangong.module.hr.service;

import cn.tiangong.framework.web.core.pojo.PageParam;
import cn.tiangong.framework.web.core.pojo.PageResult;
import cn.tiangong.module.hr.dal.dataobject.HrEmployeeDO;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface EmployeeService {

    PageResult<HrEmployeeDO> getEmployeePage(PageParam pageParam, String name, Long deptId, Integer status);

    Long createEmployee(String no, String name, Long deptId, String post, BigDecimal baseSalary,
                        String phone, String email, LocalDate entryDate);

    void updateEmployee(Long id, String name, Long deptId, String post, BigDecimal baseSalary,
                        String phone, String email, LocalDate entryDate, LocalDate leaveDate, Integer status);

    void deleteEmployee(Long id);
}
