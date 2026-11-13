package cn.tiangong.module.hr.service;

import cn.tiangong.framework.biz.common.chatter.service.ChatterService;
import cn.tiangong.framework.biz.common.order.BizOrderStatus;
import cn.tiangong.framework.biz.common.order.OrderStateChecker;
import cn.tiangong.framework.web.core.exception.ServiceException;
import cn.tiangong.framework.web.core.pojo.PageParam;
import cn.tiangong.framework.web.core.pojo.PageResult;
import cn.tiangong.module.bas.api.SerialNoApi;
import cn.tiangong.module.bpm.service.ApprovalService;
import cn.tiangong.module.hr.dal.dataobject.HrOvertimeDO;
import cn.tiangong.module.hr.dal.dataobject.HrTripDO;
import cn.tiangong.module.hr.dal.dataobject.HrResignationDO;
import cn.tiangong.module.hr.dal.mysql.HrOvertimeMapper;
import cn.tiangong.module.hr.dal.mysql.HrTripMapper;
import cn.tiangong.module.hr.dal.mysql.HrResignationMapper;
import cn.tiangong.module.hr.dal.mysql.HrEmployeeMapper;
import cn.tiangong.module.hr.dal.dataobject.HrEmployeeDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static cn.tiangong.framework.web.core.exception.ServiceException.of;

/**
 * 加班 / 出差 / 离职：统一"草稿→提交审批→通过/驳回"模式（复用轻量审批流）。
 * 离职通过后自动把员工状态置为离职。
 */
@RequiredArgsConstructor
@Service
public class HrApplyService {

    public static final String BIZ_OVERTIME = "hr_overtime";
    public static final String BIZ_TRIP = "hr_trip";
    public static final String BIZ_RESIGN = "hr_resign";

    private final HrOvertimeMapper overtimeMapper;
    private final HrTripMapper tripMapper;
    private final HrResignationMapper resignationMapper;
    private final HrEmployeeMapper employeeMapper;
    private final SerialNoApi serialNoApi;
    private final ApprovalService approvalService;
    private final ChatterService chatterService;

    // ==================== 加班 ====================

    @Transactional(rollbackFor = Exception.class)
    public Long createOvertime(Long employeeId, LocalDate otDate, java.math.BigDecimal hours, String reason) {
        HrOvertimeDO ot = new HrOvertimeDO();
        ot.setNo(serialNoApi.nextNo("hr_overtime"));
        ot.setEmployeeId(employeeId);
        ot.setOtDate(otDate != null ? otDate : LocalDate.now());
        ot.setHours(hours);
        ot.setReason(reason);
        ot.setStatus(BizOrderStatus.DRAFT.getValue());
        overtimeMapper.insert(ot);
        return ot.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void submitOvertime(Long id) {
        HrOvertimeDO ot = overtimeMapper.selectById(id);
        if (ot == null) {
            throw of(11_03, 1, "加班申请不存在");
        }
        OrderStateChecker.require(ot.getStatus(), ot.getNo(), BizOrderStatus.DRAFT);
        if (!approvalService.submit(BIZ_OVERTIME, BIZ_OVERTIME, id, ot.getNo())) {
            throw of(11_03, 2, "加班未配置审批流，请直接审核");
        }
        ot.setStatus(BizOrderStatus.APPROVING.getValue());
        overtimeMapper.updateById(ot);
    }

    @Transactional(rollbackFor = Exception.class)
    public void auditOvertime(Long id) {
        HrOvertimeDO ot = overtimeMapper.selectById(id);
        if (ot == null) {
            throw of(11_03, 1, "加班申请不存在");
        }
        OrderStateChecker.require(ot.getStatus(), ot.getNo(),
                BizOrderStatus.DRAFT, BizOrderStatus.APPROVING);
        ot.setStatus(BizOrderStatus.AUDITED.getValue());
        overtimeMapper.updateById(ot);
    }

    public void onOvertimeRejected(Long id) {
        HrOvertimeDO ot = overtimeMapper.selectById(id);
        if (ot != null && BizOrderStatus.APPROVING.getValue().equals(ot.getStatus())) {
            ot.setStatus(BizOrderStatus.VOIDED.getValue() - 10); // 50 驳回
            overtimeMapper.updateById(ot);
        }
    }

    public PageResult<HrOvertimeDO> getOvertimePage(PageParam pageParam, Long employeeId, Integer status) {
        return overtimeMapper.selectPage(pageParam, new LambdaQueryWrapper<HrOvertimeDO>()
                .eq(employeeId != null, HrOvertimeDO::getEmployeeId, employeeId)
                .eq(status != null, HrOvertimeDO::getStatus, status)
                .orderByDesc(HrOvertimeDO::getId));
    }

    public void deleteOvertime(Long id) {
        HrOvertimeDO ot = overtimeMapper.selectById(id);
        if (ot == null) {
            throw of(11_03, 1, "加班申请不存在");
        }
        OrderStateChecker.require(ot.getStatus(), ot.getNo(), BizOrderStatus.DRAFT);
        overtimeMapper.deleteById(id);
    }

    // ==================== 出差 ====================

    @Transactional(rollbackFor = Exception.class)
    public Long createTrip(Long employeeId, LocalDate start, LocalDate end, String destination, String reason) {
        if (start == null || end == null || end.isBefore(start)) {
            throw of(11_04, 1, "日期不合法");
        }
        HrTripDO trip = new HrTripDO();
        trip.setNo(serialNoApi.nextNo("hr_trip"));
        trip.setEmployeeId(employeeId);
        trip.setStartDate(start);
        trip.setEndDate(end);
        trip.setDestination(destination);
        trip.setReason(reason);
        trip.setStatus(BizOrderStatus.DRAFT.getValue());
        tripMapper.insert(trip);
        return trip.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void submitTrip(Long id) {
        HrTripDO trip = tripMapper.selectById(id);
        if (trip == null) {
            throw of(11_04, 2, "出差申请不存在");
        }
        OrderStateChecker.require(trip.getStatus(), trip.getNo(), BizOrderStatus.DRAFT);
        if (!approvalService.submit(BIZ_TRIP, BIZ_TRIP, id, trip.getNo())) {
            throw of(11_04, 3, "出差未配置审批流，请直接审核");
        }
        trip.setStatus(BizOrderStatus.APPROVING.getValue());
        tripMapper.updateById(trip);
    }

    @Transactional(rollbackFor = Exception.class)
    public void auditTrip(Long id) {
        HrTripDO trip = tripMapper.selectById(id);
        if (trip == null) {
            throw of(11_04, 2, "出差申请不存在");
        }
        OrderStateChecker.require(trip.getStatus(), trip.getNo(),
                BizOrderStatus.DRAFT, BizOrderStatus.APPROVING);
        trip.setStatus(BizOrderStatus.AUDITED.getValue());
        tripMapper.updateById(trip);
    }

    public void onTripRejected(Long id) {
        HrTripDO trip = tripMapper.selectById(id);
        if (trip != null && BizOrderStatus.APPROVING.getValue().equals(trip.getStatus())) {
            trip.setStatus(BizOrderStatus.VOIDED.getValue() - 10);
            tripMapper.updateById(trip);
        }
    }

    public PageResult<HrTripDO> getTripPage(PageParam pageParam, Long employeeId, Integer status) {
        return tripMapper.selectPage(pageParam, new LambdaQueryWrapper<HrTripDO>()
                .eq(employeeId != null, HrTripDO::getEmployeeId, employeeId)
                .eq(status != null, HrTripDO::getStatus, status)
                .orderByDesc(HrTripDO::getId));
    }

    public void deleteTrip(Long id) {
        HrTripDO trip = tripMapper.selectById(id);
        if (trip == null) {
            throw of(11_04, 2, "出差申请不存在");
        }
        OrderStateChecker.require(trip.getStatus(), trip.getNo(), BizOrderStatus.DRAFT);
        tripMapper.deleteById(id);
    }

    // ==================== 离职 ====================

    @Transactional(rollbackFor = Exception.class)
    public Long createResignation(Long employeeId, LocalDate planLeaveDate, String reason) {
        HrResignationDO rz = new HrResignationDO();
        rz.setNo(serialNoApi.nextNo("hr_resignation"));
        rz.setEmployeeId(employeeId);
        rz.setApplyDate(LocalDate.now());
        rz.setPlanLeaveDate(planLeaveDate != null ? planLeaveDate : LocalDate.now().plusMonths(1));
        rz.setReason(reason);
        rz.setStatus(BizOrderStatus.DRAFT.getValue());
        resignationMapper.insert(rz);
        return rz.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void submitResignation(Long id) {
        HrResignationDO rz = resignationMapper.selectById(id);
        if (rz == null) {
            throw of(11_05, 1, "离职申请不存在");
        }
        OrderStateChecker.require(rz.getStatus(), rz.getNo(), BizOrderStatus.DRAFT);
        if (!approvalService.submit(BIZ_RESIGN, BIZ_RESIGN, id, rz.getNo())) {
            throw of(11_05, 2, "离职未配置审批流，请直接审核");
        }
        rz.setStatus(BizOrderStatus.APPROVING.getValue());
        resignationMapper.updateById(rz);
    }

    /** 审核（直接或审批通过回调）；通过后员工转离职 */
    @Transactional(rollbackFor = Exception.class)
    public void auditResignation(Long id) {
        HrResignationDO rz = resignationMapper.selectById(id);
        if (rz == null) {
            throw of(11_05, 1, "离职申请不存在");
        }
        OrderStateChecker.require(rz.getStatus(), rz.getNo(),
                BizOrderStatus.DRAFT, BizOrderStatus.APPROVING);
        rz.setStatus(BizOrderStatus.AUDITED.getValue());
        resignationMapper.updateById(rz);
        HrEmployeeDO emp = employeeMapper.selectById(rz.getEmployeeId());
        if (emp != null) {
            emp.setStatus(1);
            emp.setLeaveDate(rz.getPlanLeaveDate());
            employeeMapper.updateById(emp);
        }
    }

    public void onResignationRejected(Long id) {
        HrResignationDO rz = resignationMapper.selectById(id);
        if (rz != null && BizOrderStatus.APPROVING.getValue().equals(rz.getStatus())) {
            rz.setStatus(BizOrderStatus.VOIDED.getValue() - 10);
            resignationMapper.updateById(rz);
        }
    }

    public PageResult<HrResignationDO> getResignationPage(PageParam pageParam, Long employeeId, Integer status) {
        return resignationMapper.selectPage(pageParam, new LambdaQueryWrapper<HrResignationDO>()
                .eq(employeeId != null, HrResignationDO::getEmployeeId, employeeId)
                .eq(status != null, HrResignationDO::getStatus, status)
                .orderByDesc(HrResignationDO::getId));
    }

    public void deleteResignation(Long id) {
        HrResignationDO rz = resignationMapper.selectById(id);
        if (rz == null) {
            throw of(11_05, 1, "离职申请不存在");
        }
        OrderStateChecker.require(rz.getStatus(), rz.getNo(), BizOrderStatus.DRAFT);
        resignationMapper.deleteById(id);
    }
}
