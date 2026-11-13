package cn.tiangong.module.hr.controller.admin;

import cn.tiangong.framework.web.core.pojo.CommonResult;
import cn.tiangong.framework.web.core.pojo.PageParam;
import cn.tiangong.framework.web.core.pojo.PageResult;
import cn.tiangong.module.hr.dal.dataobject.HrLeaveDO;
import cn.tiangong.module.hr.service.LeaveService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/admin-api/hr/leave")
public class HrLeaveController {

    private final LeaveService leaveService;

    @GetMapping("/page")
    @PreAuthorize("@ss.hasPermission('hr:leave:query')")
    public CommonResult<PageResult<HrLeaveDO>> getLeavePage(PageParam pageParam,
                                                            @RequestParam(required = false) Long employeeId,
                                                            @RequestParam(required = false) Integer status) {
        return CommonResult.success(leaveService.getLeavePage(pageParam, employeeId, status));
    }

    @PostMapping("/create")
    @PreAuthorize("@ss.hasPermission('hr:leave:create')")
    public CommonResult<Long> createLeave(@RequestParam("employeeId") Long employeeId,
                                          @RequestParam("leaveType") String leaveType,
                                          @RequestParam("startDate") String startDate,
                                          @RequestParam("endDate") String endDate,
                                          @RequestParam(value = "reason", required = false) String reason) {
        return CommonResult.success(leaveService.createLeave(employeeId, leaveType,
                LocalDate.parse(startDate), LocalDate.parse(endDate), reason));
    }

    /** 提交审批（未配置审批流则报错，请用直接审核） */
    @PostMapping("/submit-approval")
    @PreAuthorize("@ss.hasPermission('hr:leave:audit')")
    public CommonResult<Boolean> submitApproval(@RequestParam("id") Long id) {
        leaveService.submitApproval(id);
        return CommonResult.success(true);
    }

    /** 直接审核（未配置审批流时使用） */
    @PostMapping("/audit")
    @PreAuthorize("@ss.hasPermission('hr:leave:audit')")
    public CommonResult<Boolean> auditLeave(@RequestParam("id") Long id) {
        leaveService.auditLeave(id);
        return CommonResult.success(true);
    }

    @DeleteMapping("/delete")
    @PreAuthorize("@ss.hasPermission('hr:leave:delete')")
    public CommonResult<Boolean> deleteLeave(@RequestParam("id") Long id) {
        leaveService.deleteLeave(id);
        return CommonResult.success(true);
    }
}
