package cn.tiangong.module.hr.service;

import cn.tiangong.framework.biz.common.chatter.service.ChatterService;
import cn.tiangong.framework.biz.common.order.BizOrderStatus;
import cn.tiangong.framework.biz.common.order.OrderStateChecker;
import cn.tiangong.framework.web.core.exception.ServiceException;
import cn.tiangong.framework.web.core.pojo.PageParam;
import cn.tiangong.framework.web.core.pojo.PageResult;
import cn.tiangong.module.bas.api.SerialNoApi;
import cn.tiangong.module.bpm.api.ApprovalCompletedEvent;
import cn.tiangong.module.bpm.api.ApprovalRejectedEvent;
import cn.tiangong.module.bpm.service.ApprovalService;
import cn.tiangong.module.hr.dal.dataobject.HrLeaveDO;
import cn.tiangong.module.hr.dal.mysql.HrLeaveMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static cn.tiangong.framework.web.core.exception.ServiceException.of;

/**
 * 请假申请：提交后走轻量审批流，全部通过自动生效。
 */
@RequiredArgsConstructor
@Service
public class LeaveService {

    public static final String BIZ_TYPE = "hr_leave";
    private static final int STATUS_DONE = 30;
    private static final int STATUS_REJECTED = 50;

    private final HrLeaveMapper leaveMapper;
    private final SerialNoApi serialNoApi;
    private final ApprovalService approvalService;
    private final ChatterService chatterService;
    private final ApplicationEventPublisher eventPublisher;

    public PageResult<HrLeaveDO> getLeavePage(PageParam pageParam, Long employeeId, Integer status) {
        LambdaQueryWrapper<HrLeaveDO> wrapper = new LambdaQueryWrapper<HrLeaveDO>()
                .eq(employeeId != null, HrLeaveDO::getEmployeeId, employeeId)
                .eq(status != null, HrLeaveDO::getStatus, status)
                .orderByDesc(HrLeaveDO::getId);
        return leaveMapper.selectPage(pageParam, wrapper);
    }

    @Transactional(rollbackFor = Exception.class)
    public Long createLeave(Long employeeId, String leaveType, LocalDate startDate,
                            LocalDate endDate, String reason) {
        if (employeeId == null || leaveType == null || startDate == null || endDate == null) {
            throw of(11_01, 1, "员工/类型/起止日期不能为空");
        }
        if (endDate.isBefore(startDate)) {
            throw of(11_01, 2, "结束日期不能早于开始日期");
        }
        HrLeaveDO leave = new HrLeaveDO();
        leave.setNo(serialNoApi.nextNo("hr_leave"));
        leave.setEmployeeId(employeeId);
        leave.setLeaveType(leaveType);
        leave.setStartDate(startDate);
        leave.setEndDate(endDate);
        leave.setReason(reason);
        leave.setStatus(BizOrderStatus.DRAFT.getValue());
        leaveMapper.insert(leave);
        chatterService.trace(BIZ_TYPE, leave.getId(), "创建请假申请 " + leave.getNo(), null);
        return leave.getId();
    }

    /** 审核（未配置审批流时一键通过） */
    @Transactional(rollbackFor = Exception.class)
    public void auditLeave(Long id) {
        HrLeaveDO leave = getLeave(id);
        OrderStateChecker.require(leave.getStatus(), leave.getNo(),
                BizOrderStatus.DRAFT, BizOrderStatus.APPROVING);
        leave.setStatus(STATUS_DONE);
        leaveMapper.updateById(leave);
        chatterService.trace(BIZ_TYPE, id, "请假申请审核通过", null);
    }

    /** 提交审批（未配置审批流时报错，应直接审核） */
    @Transactional(rollbackFor = Exception.class)
    public void submitApproval(Long id) {
        HrLeaveDO leave = getLeave(id);
        OrderStateChecker.require(leave.getStatus(), leave.getNo(), BizOrderStatus.DRAFT);
        if (!approvalService.submit(BIZ_TYPE, BIZ_TYPE, id, leave.getNo())) {
            throw of(11_01, 3, "请假未配置审批流，请直接审核");
        }
        leave.setStatus(BizOrderStatus.APPROVING.getValue());
        leaveMapper.updateById(leave);
    }

    /** 审批驳回回调 */
    @Transactional(rollbackFor = Exception.class)
    public void onApprovalRejected(Long id) {
        HrLeaveDO leave = getLeave(id);
        OrderStateChecker.require(leave.getStatus(), leave.getNo(), BizOrderStatus.APPROVING);
        leave.setStatus(STATUS_REJECTED);
        leaveMapper.updateById(leave);
        chatterService.trace(BIZ_TYPE, id, "请假申请被驳回", null);
    }

    public void deleteLeave(Long id) {
        getLeave(id);
        leaveMapper.deleteById(id);
    }

    private HrLeaveDO getLeave(Long id) {
        HrLeaveDO leave = leaveMapper.selectById(id);
        if (leave == null) {
            throw of(11_01, 4, "请假申请不存在");
        }
        return leave;
    }
}
