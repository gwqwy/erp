package cn.tiangong.module.hr.controller.admin;

import cn.tiangong.framework.web.core.pojo.CommonResult;
import cn.tiangong.framework.web.core.pojo.PageParam;
import cn.tiangong.framework.web.core.pojo.PageResult;
import cn.tiangong.module.hr.dal.dataobject.HrContractDO;
import cn.tiangong.module.hr.dal.dataobject.HrRewardPenaltyDO;
import cn.tiangong.module.hr.service.HrExtraService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/admin-api/hr")
public class HrExtraController {

    private final HrExtraService extraService;

    // ========== 奖惩 ==========

    @GetMapping("/reward-penalty/page")
    @PreAuthorize("@ss.hasPermission('hr:reward:query')")
    public CommonResult<PageResult<HrRewardPenaltyDO>> getRpPage(PageParam pageParam,
                                                                 @RequestParam(required = false) Long employeeId,
                                                                 @RequestParam(required = false) String type) {
        return CommonResult.success(extraService.getRpPage(pageParam, employeeId, type));
    }

    @PostMapping("/reward-penalty/create")
    @PreAuthorize("@ss.hasPermission('hr:reward:create')")
    public CommonResult<Long> createRp(@RequestParam("employeeId") Long employeeId,
                                       @RequestParam("type") @NotBlank String type,
                                       @RequestParam(value = "amount", required = false) BigDecimal amount,
                                       @RequestParam(value = "rpDate", required = false) String rpDate,
                                       @RequestParam(value = "reason", required = false) String reason) {
        return CommonResult.success(extraService.createRp(employeeId, type, amount,
                rpDate == null || rpDate.isBlank() ? null : LocalDate.parse(rpDate), reason));
    }

    @DeleteMapping("/reward-penalty/delete")
    @PreAuthorize("@ss.hasPermission('hr:reward:delete')")
    public CommonResult<Boolean> deleteRp(@RequestParam("id") Long id) {
        extraService.deleteRp(id);
        return CommonResult.success(true);
    }

    // ========== 合同 ==========

    @GetMapping("/contract/page")
    @PreAuthorize("@ss.hasPermission('hr:contract:query')")
    public CommonResult<PageResult<HrContractDO>> getContractPage(PageParam pageParam,
                                                                  @RequestParam(required = false) Long employeeId,
                                                                  @RequestParam(required = false) Integer status) {
        return CommonResult.success(extraService.getContractPage(pageParam, employeeId, status));
    }

    @PostMapping("/contract/create")
    @PreAuthorize("@ss.hasPermission('hr:contract:create')")
    public CommonResult<Long> createContract(@RequestParam("employeeId") Long employeeId,
                                             @RequestParam("startDate") String startDate,
                                             @RequestParam("endDate") String endDate,
                                             @RequestParam(value = "remark", required = false) String remark) {
        return CommonResult.success(extraService.createContract(employeeId,
                LocalDate.parse(startDate), LocalDate.parse(endDate), remark));
    }

    @DeleteMapping("/contract/delete")
    @PreAuthorize("@ss.hasPermission('hr:contract:delete')")
    public CommonResult<Boolean> deleteContract(@RequestParam("id") Long id) {
        extraService.deleteContract(id);
        return CommonResult.success(true);
    }

    // ========== 算薪 ==========

    @GetMapping("/payroll/calculate")
    @PreAuthorize("@ss.hasPermission('hr:payroll:query')")
    public CommonResult<HrExtraService.PayrollBreakdown> calculate(@RequestParam("period") String period,
                                                                   @RequestParam("employeeId") Long employeeId) {
        return CommonResult.success(extraService.calculate(period, employeeId));
    }

    @PostMapping("/payroll/generate")
    @PreAuthorize("@ss.hasPermission('hr:payroll:generate')")
    public CommonResult<Long> generate(@RequestParam("period") String period,
                                       @RequestParam("employeeId") Long employeeId,
                                       @RequestParam(value = "remark", required = false) String remark) {
        return CommonResult.success(extraService.generate(period, employeeId, remark));
    }
}
