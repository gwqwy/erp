package cn.tiangong.module.hr.service;

import cn.tiangong.framework.biz.common.chatter.service.ChatterService;
import cn.tiangong.module.bas.api.SerialNoApi;
import cn.tiangong.framework.web.core.exception.ServiceException;
import cn.tiangong.framework.web.core.pojo.PageParam;
import cn.tiangong.framework.web.core.pojo.PageResult;
import cn.tiangong.module.hr.dal.dataobject.HrAttendanceDO;
import cn.tiangong.module.hr.dal.dataobject.HrContractDO;
import cn.tiangong.module.hr.dal.dataobject.HrEmployeeDO;
import cn.tiangong.module.hr.dal.dataobject.HrOvertimeDO;
import cn.tiangong.module.hr.dal.dataobject.HrRewardPenaltyDO;
import cn.tiangong.module.hr.dal.dataobject.HrSalaryDO;
import cn.tiangong.module.hr.dal.mysql.HrAttendanceMapper;
import cn.tiangong.module.hr.dal.mysql.HrContractMapper;
import cn.tiangong.module.hr.dal.mysql.HrEmployeeMapper;
import cn.tiangong.module.hr.dal.mysql.HrOvertimeMapper;
import cn.tiangong.module.hr.dal.mysql.HrRewardPenaltyMapper;
import cn.tiangong.module.hr.dal.mysql.HrSalaryMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

import static cn.tiangong.framework.web.core.exception.ServiceException.of;

/**
 * 奖惩 / 合同 CRUD + 一键算薪。
 * 算薪口径：日薪 = 基本工资 / 21.75；时薪 = 日薪 / 8；
 * 应发 = 基本工资 + 加班费(审批通过时数×时薪×1.5) + 奖励；
 * 扣款 = 缺勤×日薪 + 事假×日薪 + 处罚；实发 = 应发 - 扣款。
 */
@RequiredArgsConstructor
@Service
public class HrExtraService {

    private final HrRewardPenaltyMapper rpMapper;
    private final HrContractMapper contractMapper;
    private final HrEmployeeMapper employeeMapper;
    private final HrOvertimeMapper overtimeMapper;
    private final HrAttendanceMapper attendanceMapper;
    private final HrSalaryMapper salaryMapper;
    private final SerialNoApi serialNoApi;
    private final ChatterService chatterService;

    // ==================== 奖惩 ====================

    public PageResult<HrRewardPenaltyDO> getRpPage(PageParam pageParam, Long employeeId, String type) {
        return rpMapper.selectPage(pageParam, new LambdaQueryWrapper<HrRewardPenaltyDO>()
                .eq(employeeId != null, HrRewardPenaltyDO::getEmployeeId, employeeId)
                .eq(type != null && !type.isBlank(), HrRewardPenaltyDO::getType, type)
                .orderByDesc(HrRewardPenaltyDO::getId));
    }

    @Transactional(rollbackFor = Exception.class)
    public Long createRp(Long employeeId, String type, java.math.BigDecimal amount,
                         LocalDate rpDate, String reason) {
        if (employeeId == null || type == null || type.isBlank()) {
            throw of(11_06, 1, "员工与类型不能为空");
        }
        HrRewardPenaltyDO rp = new HrRewardPenaltyDO();
        rp.setNo(serialNoApi.nextNo("hr_reward_penalty"));
        rp.setEmployeeId(employeeId);
        rp.setType(type);
        rp.setAmount(amount == null ? java.math.BigDecimal.ZERO : amount.abs());
        rp.setRpDate(rpDate != null ? rpDate : LocalDate.now());
        rp.setReason(reason);
        rpMapper.insert(rp);
        return rp.getId();
    }

    public void deleteRp(Long id) {
        if (rpMapper.selectById(id) == null) {
            throw of(11_06, 2, "奖惩记录不存在");
        }
        rpMapper.deleteById(id);
    }

    // ==================== 合同 ====================

    public PageResult<HrContractDO> getContractPage(PageParam pageParam, Long employeeId, Integer status) {
        return contractMapper.selectPage(pageParam, new LambdaQueryWrapper<HrContractDO>()
                .eq(employeeId != null, HrContractDO::getEmployeeId, employeeId)
                .eq(status != null, HrContractDO::getStatus, status)
                .orderByDesc(HrContractDO::getId));
    }

    @Transactional(rollbackFor = Exception.class)
    public Long createContract(Long employeeId, LocalDate start, LocalDate end, String remark) {
        if (employeeId == null || start == null || end == null || end.isBefore(start)) {
            throw of(11_07, 1, "员工与合同期限不合法");
        }
        HrContractDO contract = new HrContractDO();
        contract.setNo(serialNoApi.nextNo("hr_contract"));
        contract.setEmployeeId(employeeId);
        contract.setStartDate(start);
        contract.setEndDate(end);
        // 30 天内到期标"即将到期"语义由前端展示，status 仍为履行中
        contract.setStatus(0);
        contract.setRemark(remark);
        contractMapper.insert(contract);
        return contract.getId();
    }

    public void deleteContract(Long id) {
        if (contractMapper.selectById(id) == null) {
            throw of(11_07, 2, "合同不存在");
        }
        contractMapper.deleteById(id);
    }

    // ==================== 算薪 ====================

    /** 试算 breakdown（不落库） */
    public PayrollBreakdown calculate(String period, Long employeeId) {
        HrEmployeeDO emp = employeeMapper.selectById(employeeId);
        if (emp == null) {
            throw of(11_08, 1, "员工不存在");
        }
        BigDecimal base = emp.getBaseSalary() == null ? java.math.BigDecimal.ZERO : emp.getBaseSalary();
        BigDecimal daily = base.divide(new BigDecimal("21.75"), 4, RoundingMode.HALF_UP);
        BigDecimal hourly = daily.divide(new BigDecimal("8"), 4, RoundingMode.HALF_UP);

        // 审批通过的加班时数
        LocalDate begin = LocalDate.parse(period + "-01");
        LocalDate end = begin.plusMonths(1).minusDays(1);
        List<HrOvertimeDO> ots = overtimeMapper.selectList(new LambdaQueryWrapper<HrOvertimeDO>()
                .eq(HrOvertimeDO::getEmployeeId, employeeId)
                .eq(HrOvertimeDO::getStatus, 30)
                .ge(HrOvertimeDO::getOtDate, begin).le(HrOvertimeDO::getOtDate, end));
        BigDecimal otHours = ots.stream().map(HrOvertimeDO::getHours)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal otAmount = otHours.multiply(hourly).multiply(new BigDecimal("1.5"))
                .setScale(2, RoundingMode.HALF_UP);

        // 缺勤 / 事假天数
        List<HrAttendanceDO> atts = attendanceMapper.selectList(new LambdaQueryWrapper<HrAttendanceDO>()
                .eq(HrAttendanceDO::getEmployeeId, employeeId)
                .ge(HrAttendanceDO::getAttDate, begin).le(HrAttendanceDO::getAttDate, end));
        long absent = atts.stream().filter(a -> a.getStatus() != null && a.getStatus() == 3).count();
        long personalLeave = atts.stream().filter(a -> a.getStatus() != null && a.getStatus() == 4).count();
        BigDecimal absenceDeduction = new BigDecimal(absent + personalLeave).multiply(daily)
                .setScale(2, RoundingMode.HALF_UP);

        // 奖惩
        List<HrRewardPenaltyDO> rps = rpMapper.selectList(new LambdaQueryWrapper<HrRewardPenaltyDO>()
                .eq(HrRewardPenaltyDO::getEmployeeId, employeeId)
                .ge(HrRewardPenaltyDO::getRpDate, begin).le(HrRewardPenaltyDO::getRpDate, end));
        BigDecimal reward = rps.stream().filter(r -> "reward".equals(r.getType()))
                .map(HrRewardPenaltyDO::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal penalty = rps.stream().filter(r -> "penalty".equals(r.getType()))
                .map(HrRewardPenaltyDO::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal gross = base.add(otAmount).add(reward);
        BigDecimal deduction = absenceDeduction.add(penalty);
        BigDecimal net = gross.subtract(deduction);

        return new PayrollBreakdown(period, employeeId, emp.getName(), base, otHours, otAmount,
                absent + personalLeave, absenceDeduction, reward, penalty, gross, deduction, net);
    }

    /** 试算并生成薪资记录（写 hr_salary） */
    @Transactional(rollbackFor = Exception.class)
    public Long generate(String period, Long employeeId, String remark) {
        PayrollBreakdown b = calculate(period, employeeId);
        Long dup = salaryMapper.selectCount(new LambdaQueryWrapper<HrSalaryDO>()
                .eq(HrSalaryDO::getPeriod, period)
                .eq(HrSalaryDO::getEmployeeId, employeeId));
        if (dup > 0) {
            throw of(11_08, 2, "该员工此期间已有薪资记录，请先删除后重算");
        }
        HrSalaryDO salary = new HrSalaryDO();
        salary.setPeriod(period);
        salary.setEmployeeId(employeeId);
        salary.setGrossAmount(b.gross());
        salary.setDeduction(b.deduction());
        salary.setNetAmount(b.net());
        salary.setRemark(remark);
        salaryMapper.insert(salary);
        chatterService.trace("hr_payroll", salary.getId(),
                String.format("生成 %s 期薪资：应发 %s，扣款 %s，实发 %s",
                        period, b.gross(), b.deduction(), b.net()), null);
        return salary.getId();
    }

    public record PayrollBreakdown(String period, Long employeeId, String employeeName,
                                   BigDecimal baseSalary, BigDecimal overtimeHours, BigDecimal overtimeAmount,
                                   Long absenceDays, BigDecimal absenceDeduction,
                                   BigDecimal rewardAmount, BigDecimal penaltyAmount,
                                   BigDecimal gross, BigDecimal deduction, BigDecimal net) {
    }
}
