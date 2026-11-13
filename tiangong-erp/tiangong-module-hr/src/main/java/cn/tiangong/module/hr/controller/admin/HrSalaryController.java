package cn.tiangong.module.hr.controller.admin;

import cn.tiangong.framework.web.core.pojo.CommonResult;
import cn.tiangong.framework.web.core.pojo.PageParam;
import cn.tiangong.framework.web.core.pojo.PageResult;
import cn.tiangong.module.hr.dal.dataobject.HrSalaryDO;
import cn.tiangong.module.hr.dal.mysql.HrSalaryMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/admin-api/hr/salary")
public class HrSalaryController {

    private final HrSalaryMapper salaryMapper;

    @GetMapping("/page")
    @PreAuthorize("@ss.hasPermission('hr:salary:query')")
    public CommonResult<PageResult<HrSalaryDO>> getSalaryPage(PageParam pageParam,
                                                              @RequestParam(required = false) String period,
                                                              @RequestParam(required = false) Long employeeId) {
        LambdaQueryWrapper<HrSalaryDO> wrapper = new LambdaQueryWrapper<HrSalaryDO>()
                .eq(period != null && !period.isBlank(), HrSalaryDO::getPeriod, period)
                .eq(employeeId != null, HrSalaryDO::getEmployeeId, employeeId)
                .orderByDesc(HrSalaryDO::getId);
        return CommonResult.success(salaryMapper.selectPage(pageParam, wrapper));
    }

    @PostMapping("/create")
    @PreAuthorize("@ss.hasPermission('hr:salary:create')")
    public CommonResult<Long> createSalary(@RequestParam("period") @NotBlank String period,
                                           @RequestParam("employeeId") @NotNull Long employeeId,
                                           @RequestParam("grossAmount") @NotNull BigDecimal grossAmount,
                                           @RequestParam(value = "deduction", required = false) BigDecimal deduction,
                                           @RequestParam(value = "remark", required = false) String remark) {
        HrSalaryDO salary = new HrSalaryDO();
        salary.setPeriod(period);
        salary.setEmployeeId(employeeId);
        salary.setGrossAmount(grossAmount);
        salary.setDeduction(deduction == null ? BigDecimal.ZERO : deduction);
        salary.setNetAmount(grossAmount.subtract(salary.getDeduction()));
        salary.setRemark(remark);
        salaryMapper.insert(salary);
        return CommonResult.success(salary.getId());
    }

    @DeleteMapping("/delete")
    @PreAuthorize("@ss.hasPermission('hr:salary:delete')")
    public CommonResult<Boolean> deleteSalary(@RequestParam("id") Long id) {
        salaryMapper.deleteById(id);
        return CommonResult.success(true);
    }
}
