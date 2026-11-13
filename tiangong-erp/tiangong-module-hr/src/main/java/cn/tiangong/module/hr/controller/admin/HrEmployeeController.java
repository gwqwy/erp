package cn.tiangong.module.hr.controller.admin;

import cn.tiangong.framework.web.core.pojo.CommonResult;
import cn.tiangong.framework.web.core.pojo.PageParam;
import cn.tiangong.framework.web.core.pojo.PageResult;
import cn.tiangong.module.hr.dal.dataobject.HrEmployeeDO;
import cn.tiangong.module.hr.service.EmployeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;

@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/admin-api/hr/employee")
public class HrEmployeeController {

    private final EmployeeService employeeService;

    @GetMapping("/page")
    @PreAuthorize("@ss.hasPermission('hr:employee:query')")
    public CommonResult<PageResult<HrEmployeeDO>> getEmployeePage(PageParam pageParam,
                                                                  @RequestParam(required = false) String name,
                                                                  @RequestParam(required = false) Long deptId,
                                                                  @RequestParam(required = false) Integer status) {
        return CommonResult.success(employeeService.getEmployeePage(pageParam, name, deptId, status));
    }

    @PostMapping("/create")
    @PreAuthorize("@ss.hasPermission('hr:employee:create')")
    public CommonResult<Long> createEmployee(@Valid @RequestBody EmployeeReqVO reqVO) {
        return CommonResult.success(employeeService.createEmployee(reqVO.no(), reqVO.name(), reqVO.deptId(),
                reqVO.post(), reqVO.baseSalary(), reqVO.phone(), reqVO.email(), reqVO.entryDate()));
    }

    @PutMapping("/update")
    @PreAuthorize("@ss.hasPermission('hr:employee:update')")
    public CommonResult<Boolean> updateEmployee(@Valid @RequestBody EmployeeUpdateReqVO reqVO) {
        employeeService.updateEmployee(reqVO.id(), reqVO.name(), reqVO.deptId(), reqVO.post(),
                reqVO.baseSalary(), reqVO.phone(), reqVO.email(), reqVO.entryDate(), reqVO.leaveDate(), reqVO.status());
        return CommonResult.success(true);
    }

    @DeleteMapping("/delete")
    @PreAuthorize("@ss.hasPermission('hr:employee:delete')")
    public CommonResult<Boolean> deleteEmployee(@RequestParam("id") Long id) {
        employeeService.deleteEmployee(id);
        return CommonResult.success(true);
    }

    public record EmployeeReqVO(String no, String name, Long deptId, String post, BigDecimal baseSalary,
                                String phone, String email, LocalDate entryDate) {
    }

    public record EmployeeUpdateReqVO(Long id, String name, Long deptId, String post, BigDecimal baseSalary,
                                      String phone, String email, LocalDate entryDate,
                                      LocalDate leaveDate, Integer status) {
    }
}
