package cn.tiangong.module.hr.controller.admin;

import cn.tiangong.framework.web.core.pojo.CommonResult;
import cn.tiangong.framework.web.core.pojo.PageParam;
import cn.tiangong.framework.web.core.pojo.PageResult;
import cn.tiangong.module.hr.dal.dataobject.HrAttendanceDO;
import cn.tiangong.module.hr.service.AttendanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/admin-api/hr/attendance")
public class HrAttendanceController {

    private final AttendanceService attendanceService;

    @GetMapping("/page")
    @PreAuthorize("@ss.hasPermission('hr:attendance:query')")
    public CommonResult<PageResult<HrAttendanceDO>> getRecordPage(PageParam pageParam,
                                                                  @RequestParam(required = false) Long employeeId,
                                                                  @RequestParam(required = false) String begin,
                                                                  @RequestParam(required = false) String end) {
        return CommonResult.success(attendanceService.getRecordPage(pageParam, employeeId,
                begin == null || begin.isBlank() ? null : LocalDate.parse(begin),
                end == null || end.isBlank() ? null : LocalDate.parse(end)));
    }

    @PostMapping("/check-in")
    @PreAuthorize("@ss.hasPermission('hr:attendance:clock')")
    public CommonResult<Long> checkIn(@RequestParam("employeeId") Long employeeId) {
        return CommonResult.success(attendanceService.checkIn(employeeId));
    }

    @PostMapping("/check-out")
    @PreAuthorize("@ss.hasPermission('hr:attendance:clock')")
    public CommonResult<Long> checkOut(@RequestParam("employeeId") Long employeeId) {
        return CommonResult.success(attendanceService.checkOut(employeeId));
    }

    @PostMapping("/save")
    @PreAuthorize("@ss.hasPermission('hr:attendance:create')")
    public CommonResult<Boolean> saveRecord(@RequestParam("employeeId") Long employeeId,
                                            @RequestParam("attDate") String attDate,
                                            @RequestParam(value = "checkIn", required = false) String checkIn,
                                            @RequestParam(value = "checkOut", required = false) String checkOut,
                                            @RequestParam(value = "status", required = false) Integer status,
                                            @RequestParam(value = "remark", required = false) String remark) {
        attendanceService.saveRecord(employeeId, LocalDate.parse(attDate),
                checkIn == null || checkIn.isBlank() ? null : LocalTime.parse(checkIn),
                checkOut == null || checkOut.isBlank() ? null : LocalTime.parse(checkOut),
                status, remark);
        return CommonResult.success(true);
    }

    @GetMapping("/summary")
    @PreAuthorize("@ss.hasPermission('hr:attendance:query')")
    public CommonResult<List<Map<String, Object>>> getMonthlySummary(@RequestParam("period") String period) {
        return CommonResult.success(attendanceService.monthlySummary(period));
    }
}
