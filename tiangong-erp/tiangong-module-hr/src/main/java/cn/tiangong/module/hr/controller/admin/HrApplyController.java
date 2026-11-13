package cn.tiangong.module.hr.controller.admin;

import cn.tiangong.framework.web.core.pojo.CommonResult;
import cn.tiangong.framework.web.core.pojo.PageParam;
import cn.tiangong.framework.web.core.pojo.PageResult;
import cn.tiangong.module.hr.dal.dataobject.HrOvertimeDO;
import cn.tiangong.module.hr.dal.dataobject.HrResignationDO;
import cn.tiangong.module.hr.dal.dataobject.HrTripDO;
import cn.tiangong.module.hr.service.HrApplyService;
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

/** 加班 / 出差 / 离职：申请、提交审批、直接审核。 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/admin-api/hr")
public class HrApplyController {

    private final HrApplyService applyService;

    // ========== 加班 ==========

    @GetMapping("/overtime/page")
    @PreAuthorize("@ss.hasPermission('hr:overtime:query')")
    public CommonResult<PageResult<HrOvertimeDO>> getOvertimePage(PageParam pageParam,
                                                                  @RequestParam(required = false) Long employeeId,
                                                                  @RequestParam(required = false) Integer status) {
        return CommonResult.success(applyService.getOvertimePage(pageParam, employeeId, status));
    }

    @PostMapping("/overtime/create")
    @PreAuthorize("@ss.hasPermission('hr:overtime:create')")
    public CommonResult<Long> createOvertime(@RequestParam("employeeId") Long employeeId,
                                             @RequestParam(value = "otDate", required = false) String otDate,
                                             @RequestParam("hours") BigDecimal hours,
                                             @RequestParam(value = "reason", required = false) String reason) {
        return CommonResult.success(applyService.createOvertime(employeeId,
                otDate == null || otDate.isBlank() ? null : LocalDate.parse(otDate), hours, reason));
    }

    @PostMapping("/overtime/submit-approval")
    @PreAuthorize("@ss.hasPermission('hr:overtime:audit')")
    public CommonResult<Boolean> submitOvertime(@RequestParam("id") Long id) {
        applyService.submitOvertime(id);
        return CommonResult.success(true);
    }

    @PostMapping("/overtime/audit")
    @PreAuthorize("@ss.hasPermission('hr:overtime:audit')")
    public CommonResult<Boolean> auditOvertime(@RequestParam("id") Long id) {
        applyService.auditOvertime(id);
        return CommonResult.success(true);
    }

    @DeleteMapping("/overtime/delete")
    @PreAuthorize("@ss.hasPermission('hr:overtime:audit')")
    public CommonResult<Boolean> deleteOvertime(@RequestParam("id") Long id) {
        applyService.deleteOvertime(id);
        return CommonResult.success(true);
    }

    // ========== 出差 ==========

    @GetMapping("/trip/page")
    @PreAuthorize("@ss.hasPermission('hr:trip:query')")
    public CommonResult<PageResult<HrTripDO>> getTripPage(PageParam pageParam,
                                                          @RequestParam(required = false) Long employeeId,
                                                          @RequestParam(required = false) Integer status) {
        return CommonResult.success(applyService.getTripPage(pageParam, employeeId, status));
    }

    @PostMapping("/trip/create")
    @PreAuthorize("@ss.hasPermission('hr:trip:create')")
    public CommonResult<Long> createTrip(@RequestParam("employeeId") Long employeeId,
                                         @RequestParam("startDate") String startDate,
                                         @RequestParam("endDate") String endDate,
                                         @RequestParam("destination") String destination,
                                         @RequestParam(value = "reason", required = false) String reason) {
        return CommonResult.success(applyService.createTrip(employeeId,
                LocalDate.parse(startDate), LocalDate.parse(endDate), destination, reason));
    }

    @PostMapping("/trip/submit-approval")
    @PreAuthorize("@ss.hasPermission('hr:trip:audit')")
    public CommonResult<Boolean> submitTrip(@RequestParam("id") Long id) {
        applyService.submitTrip(id);
        return CommonResult.success(true);
    }

    @PostMapping("/trip/audit")
    @PreAuthorize("@ss.hasPermission('hr:trip:audit')")
    public CommonResult<Boolean> auditTrip(@RequestParam("id") Long id) {
        applyService.auditTrip(id);
        return CommonResult.success(true);
    }

    @DeleteMapping("/trip/delete")
    @PreAuthorize("@ss.hasPermission('hr:trip:audit')")
    public CommonResult<Boolean> deleteTrip(@RequestParam("id") Long id) {
        applyService.deleteTrip(id);
        return CommonResult.success(true);
    }

    // ========== 离职 ==========

    @GetMapping("/resignation/page")
    @PreAuthorize("@ss.hasPermission('hr:resign:query')")
    public CommonResult<PageResult<HrResignationDO>> getResignationPage(PageParam pageParam,
                                                                        @RequestParam(required = false) Long employeeId,
                                                                        @RequestParam(required = false) Integer status) {
        return CommonResult.success(applyService.getResignationPage(pageParam, employeeId, status));
    }

    @PostMapping("/resignation/create")
    @PreAuthorize("@ss.hasPermission('hr:resign:create')")
    public CommonResult<Long> createResignation(@RequestParam("employeeId") Long employeeId,
                                                @RequestParam(value = "planLeaveDate", required = false) String planLeaveDate,
                                                @RequestParam(value = "reason", required = false) String reason) {
        return CommonResult.success(applyService.createResignation(employeeId,
                planLeaveDate == null || planLeaveDate.isBlank() ? null : LocalDate.parse(planLeaveDate), reason));
    }

    @PostMapping("/resignation/submit-approval")
    @PreAuthorize("@ss.hasPermission('hr:resign:audit')")
    public CommonResult<Boolean> submitResignation(@RequestParam("id") Long id) {
        applyService.submitResignation(id);
        return CommonResult.success(true);
    }

    @PostMapping("/resignation/audit")
    @PreAuthorize("@ss.hasPermission('hr:resign:audit')")
    public CommonResult<Boolean> auditResignation(@RequestParam("id") Long id) {
        applyService.auditResignation(id);
        return CommonResult.success(true);
    }

    @DeleteMapping("/resignation/delete")
    @PreAuthorize("@ss.hasPermission('hr:resign:audit')")
    public CommonResult<Boolean> deleteResignation(@RequestParam("id") Long id) {
        applyService.deleteResignation(id);
        return CommonResult.success(true);
    }
}
