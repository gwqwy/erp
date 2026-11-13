package cn.tiangong.module.hr.service;

import cn.tiangong.framework.web.core.pojo.PageParam;
import cn.tiangong.framework.web.core.pojo.PageResult;
import cn.tiangong.module.hr.dal.dataobject.HrEmployeeDO;
import cn.tiangong.module.hr.dal.mysql.HrEmployeeMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;

import static cn.tiangong.framework.web.core.exception.ServiceException.of;

@RequiredArgsConstructor
@Service
public class EmployeeServiceImpl implements EmployeeService {

    private final HrEmployeeMapper employeeMapper;

    @Override
    public PageResult<HrEmployeeDO> getEmployeePage(PageParam pageParam, String name, Long deptId, Integer status) {
        LambdaQueryWrapper<HrEmployeeDO> wrapper = new LambdaQueryWrapper<HrEmployeeDO>()
                .like(name != null && !name.isBlank(), HrEmployeeDO::getName, name)
                .eq(deptId != null, HrEmployeeDO::getDeptId, deptId)
                .eq(status != null, HrEmployeeDO::getStatus, status)
                .orderByAsc(HrEmployeeDO::getNo);
        return employeeMapper.selectPage(pageParam, wrapper);
    }

    @Override
    public Long createEmployee(String no, String name, Long deptId, String post, BigDecimal baseSalary,
                               String phone, String email, LocalDate entryDate) {
        if (name == null || name.isBlank()) {
            throw of(11_00, 1, "员工姓名不能为空");
        }
        HrEmployeeDO employee = new HrEmployeeDO();
        employee.setNo(no != null && !no.isBlank() ? no : "E" + System.currentTimeMillis() % 100000);
        employee.setName(name);
        employee.setDeptId(deptId);
        employee.setPost(post);
        employee.setBaseSalary(baseSalary);
        employee.setPhone(phone);
        employee.setEmail(email);
        employee.setEntryDate(entryDate != null ? entryDate : LocalDate.now());
        employee.setStatus(0);
        employeeMapper.insert(employee);
        return employee.getId();
    }

    @Override
    public void updateEmployee(Long id, String name, Long deptId, String post, BigDecimal baseSalary,
                               String phone, String email, LocalDate entryDate, LocalDate leaveDate, Integer status) {
        HrEmployeeDO employee = employeeMapper.selectById(id);
        if (employee == null) {
            throw of(11_00, 2, "员工不存在");
        }
        employee.setName(name != null ? name : employee.getName());
        employee.setDeptId(deptId != null ? deptId : employee.getDeptId());
        employee.setPost(post);
        employee.setBaseSalary(baseSalary != null ? baseSalary : employee.getBaseSalary());
        employee.setPhone(phone);
        employee.setEmail(email);
        employee.setEntryDate(entryDate != null ? entryDate : employee.getEntryDate());
        employee.setLeaveDate(leaveDate);
        employee.setStatus(status != null ? status : employee.getStatus());
        employeeMapper.updateById(employee);
    }

    @Override
    public void deleteEmployee(Long id) {
        if (employeeMapper.selectById(id) == null) {
            throw of(11_00, 2, "员工不存在");
        }
        employeeMapper.deleteById(id);
    }
}
