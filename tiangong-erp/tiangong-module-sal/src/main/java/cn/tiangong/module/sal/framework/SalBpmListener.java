package cn.tiangong.module.sal.framework;

import cn.tiangong.framework.biz.common.chatter.service.ChatterService;
import cn.tiangong.module.bpm.api.ApprovalCompletedEvent;
import cn.tiangong.module.bpm.api.ApprovalRejectedEvent;
import cn.tiangong.module.sal.service.order.SaleOrderService;
import cn.tiangong.module.sal.service.outbound.SaleOutboundService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 审批完成/驳回回调：审批流走完后自动执行销售单据的审核过账逻辑。
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class SalBpmListener {

    private final SaleOrderService orderService;
    private final SaleOutboundService outboundService;
    private final ChatterService chatterService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void onCompleted(ApprovalCompletedEvent event) {
        try {
            switch (event.bizType()) {
                case "sal_order" -> orderService.auditOrder(event.bizId());
                case "sal_outbound" -> outboundService.auditOutbound(event.bizId());
                default -> { return; }
            }
            chatterService.trace(event.chatterBizType(), event.bizId(),
                    "审批全部通过，单据已自动审核", null);
        } catch (Exception e) {
            log.error("[onCompleted] 审批完成回调失败: {} bizId={}", event.bizType(), event.bizId(), e);
            chatterService.trace(event.chatterBizType(), event.bizId(),
                    "审批完成回调失败：" + e.getMessage(), null);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void onRejected(ApprovalRejectedEvent event) {
        try {
            switch (event.bizType()) {
                case "sal_order" -> orderService.onApprovalRejected(event.bizId());
                case "sal_outbound" -> outboundService.onApprovalRejected(event.bizId());
                default -> { return; }
            }
        } catch (Exception e) {
            log.error("[onRejected] 审批驳回回调失败: {} bizId={}", event.bizType(), event.bizId(), e);
        }
    }
}
