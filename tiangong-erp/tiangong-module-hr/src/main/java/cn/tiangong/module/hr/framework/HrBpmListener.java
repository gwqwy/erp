package cn.tiangong.module.hr.framework;

import cn.tiangong.framework.biz.common.chatter.service.ChatterService;
import cn.tiangong.module.bpm.api.ApprovalCompletedEvent;
import cn.tiangong.module.bpm.api.ApprovalRejectedEvent;
import cn.tiangong.module.hr.service.HrApplyService;
import cn.tiangong.module.hr.service.LeaveService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 人事审批回调：请假 / 加班 / 出差 / 离职 全部通过 → 生效；驳回 → 驳回状态。
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class HrBpmListener {

    private final LeaveService leaveService;
    private final HrApplyService applyService;
    private final ChatterService chatterService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void onCompleted(ApprovalCompletedEvent event) {
        try {
            switch (event.bizType()) {
                case "hr_leave" -> leaveService.auditLeave(event.bizId());
                case "hr_overtime" -> applyService.auditOvertime(event.bizId());
                case "hr_trip" -> applyService.auditTrip(event.bizId());
                case "hr_resign" -> applyService.auditResignation(event.bizId());
                default -> { return; }
            }
            chatterService.trace(event.chatterBizType(), event.bizId(), "审批全部通过，申请生效", null);
        } catch (Exception e) {
            log.error("[onCompleted] 人事审批回调失败: {} bizId={}", event.bizType(), event.bizId(), e);
            chatterService.trace(event.chatterBizType(), event.bizId(),
                    "审批完成回调失败：" + e.getMessage(), null);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void onRejected(ApprovalRejectedEvent event) {
        try {
            switch (event.bizType()) {
                case "hr_leave" -> leaveService.onApprovalRejected(event.bizId());
                case "hr_overtime" -> applyService.onOvertimeRejected(event.bizId());
                case "hr_trip" -> applyService.onTripRejected(event.bizId());
                case "hr_resign" -> applyService.onResignationRejected(event.bizId());
                default -> { return; }
            }
        } catch (Exception e) {
            log.error("[onRejected] 人事驳回回调失败: {} bizId={}", event.bizType(), event.bizId(), e);
        }
    }
}
