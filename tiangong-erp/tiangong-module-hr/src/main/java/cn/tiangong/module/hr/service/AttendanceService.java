package cn.tiangong.module.hr.service;

import cn.tiangong.framework.web.core.exception.ServiceException;
import cn.tiangong.framework.web.core.pojo.PageParam;
import cn.tiangong.framework.web.core.pojo.PageResult;
import cn.tiangong.module.hr.dal.dataobject.HrAttendanceDO;
import cn.tiangong.module.hr.dal.dataobject.HrEmployeeDO;
import cn.tiangong.module.hr.dal.mysql.HrAttendanceMapper;
import cn.tiangong.module.hr.dal.mysql.HrEmployeeMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static cn.tiangong.framework.web.core.exception.ServiceException.of;

/**
 * 考勤：打卡（上班/下班）、补录、月度汇总。
 * 规则：上班打卡晚于 09:00 记迟到；下班早于 18:00 记早退；两者取先判定的一个。
 */
@RequiredArgsConstructor
@Service
public class AttendanceService {

    public static final LocalTime WORK_START = LocalTime.of(9, 0);
    public static final LocalTime WORK_END = LocalTime.of(18, 0);

    public static final int STATUS_NORMAL = 0;
    public static final int STATUS_LATE = 1;
    public static final int STATUS_EARLY = 2;

    private final HrAttendanceMapper attendanceMapper;
    private final HrEmployeeMapper employeeMapper;

    public PageResult<HrAttendanceDO> getRecordPage(PageParam pageParam, Long employeeId,
                                                    LocalDate begin, LocalDate end) {
        LambdaQueryWrapper<HrAttendanceDO> wrapper = new LambdaQueryWrapper<HrAttendanceDO>()
                .eq(employeeId != null, HrAttendanceDO::getEmployeeId, employeeId)
                .ge(begin != null, HrAttendanceDO::getAttDate, begin)
                .le(end != null, HrAttendanceDO::getAttDate, end)
                .orderByDesc(HrAttendanceDO::getAttDate);
        return attendanceMapper.selectPage(pageParam, wrapper);
    }

    /** 上班打卡：当天无记录则创建并判迟到 */
    public Long checkIn(Long employeeId) {
        LocalDate today = LocalDate.now();
        HrAttendanceDO record = getTodayRecord(employeeId, today);
        if (record != null && record.getCheckIn() != null) {
            throw of(11_02, 1, "今天已打过上班卡");
        }
        LocalTime now = LocalTime.now().withNano(0);
        int status = now.isAfter(WORK_START) ? STATUS_LATE : STATUS_NORMAL;
        if (record == null) {
            record = new HrAttendanceDO();
            record.setEmployeeId(employeeId);
            record.setAttDate(today);
            record.setCheckIn(now);
            record.setStatus(status);
            attendanceMapper.insert(record);
        } else {
            record.setCheckIn(now);
            record.setStatus(status);
            attendanceMapper.updateById(record);
        }
        return record.getId();
    }

    /** 下班打卡：需已打上班卡；早于 18:00 且状态为正常则记早退 */
    public Long checkOut(Long employeeId) {
        LocalDate today = LocalDate.now();
        HrAttendanceDO record = getTodayRecord(employeeId, today);
        if (record == null || record.getCheckIn() == null) {
            throw of(11_02, 2, "今天尚未打上班卡");
        }
        if (record.getCheckOut() != null) {
            throw of(11_02, 3, "今天已打过下班卡");
        }
        LocalTime now = LocalTime.now().withNano(0);
        record.setCheckOut(now);
        if (STATUS_NORMAL == record.getStatus() && now.isBefore(WORK_END)) {
            record.setStatus(STATUS_EARLY);
        }
        attendanceMapper.updateById(record);
        return record.getId();
    }

    /** 补录/修改 */
    public void saveRecord(Long employeeId, LocalDate attDate, LocalTime checkIn, LocalTime checkOut,
                           Integer status, String remark) {
        if (employeeId == null || attDate == null) {
            throw of(11_02, 4, "员工与日期不能为空");
        }
        HrAttendanceDO record = getTodayRecord(employeeId, attDate);
        if (record == null) {
            record = new HrAttendanceDO();
            record.setEmployeeId(employeeId);
            record.setAttDate(attDate);
        }
        record.setCheckIn(checkIn);
        record.setCheckOut(checkOut);
        record.setStatus(status != null ? status : STATUS_NORMAL);
        record.setRemark(remark);
        if (record.getId() == null) {
            attendanceMapper.insert(record);
        } else {
            attendanceMapper.updateById(record);
        }
    }

    /** 月度汇总：每名员工 出勤/迟到/早退/缺勤/事假/年假 天数 + 加班时数 */
    public List<Map<String, Object>> monthlySummary(String period) {
        LocalDate begin = LocalDate.parse(period + "-01");
        LocalDate end = begin.plusMonths(1).minusDays(1);
        List<HrEmployeeDO> employees = employeeMapper.selectList(null);
        List<Map<String, Object>> result = new ArrayList<>();
        for (HrEmployeeDO emp : employees) {
            List<HrAttendanceDO> records = attendanceMapper.selectList(
                    new LambdaQueryWrapper<HrAttendanceDO>()
                            .eq(HrAttendanceDO::getEmployeeId, emp.getId())
                            .ge(HrAttendanceDO::getAttDate, begin)
                            .le(HrAttendanceDO::getAttDate, end));
            Map<Integer, Long> byStatus = new HashMap<>();
            for (HrAttendanceDO r : records) {
                byStatus.merge(r.getStatus(), 1L, Long::sum);
            }
            Map<String, Object> row = new HashMap<>();
            row.put("employeeId", String.valueOf(emp.getId()));
            row.put("employeeName", emp.getName());
            row.put("presentDays", byStatus.getOrDefault(STATUS_NORMAL, 0L)
                    + byStatus.getOrDefault(STATUS_LATE, 0L) + byStatus.getOrDefault(STATUS_EARLY, 0L));
            row.put("lateDays", byStatus.getOrDefault(STATUS_LATE, 0L));
            row.put("earlyDays", byStatus.getOrDefault(STATUS_EARLY, 0L));
            row.put("absentDays", byStatus.getOrDefault(3, 0L));
            row.put("personalLeaveDays", byStatus.getOrDefault(4, 0L));
            row.put("annualLeaveDays", byStatus.getOrDefault(5, 0L));
            result.add(row);
        }
        return result;
    }

    private HrAttendanceDO getTodayRecord(Long employeeId, LocalDate date) {
        return attendanceMapper.selectOne(new LambdaQueryWrapper<HrAttendanceDO>()
                .eq(HrAttendanceDO::getEmployeeId, employeeId)
                .eq(HrAttendanceDO::getAttDate, date).last("LIMIT 1"));
    }
}
