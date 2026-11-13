package cn.tiangong.module.pur.framework;

import cn.tiangong.framework.biz.common.chatter.service.ChatterService;
import cn.tiangong.framework.biz.common.order.BizOrderStatus;
import cn.tiangong.module.bpm.api.ApprovalCompletedEvent;
import cn.tiangong.module.bpm.api.ApprovalRejectedEvent;
import cn.tiangong.module.pur.service.inbound.PurchaseInboundService;
import cn.tiangong.module.pur.service.order.PurchaseOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 审批完成/驳回回调：审批流走完后自动执行采购单据的审核过账逻辑。
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class PurBpmListener {

    private final PurchaseOrderService orderService;
    private final PurchaseInboundService inboundService;
    private final ChatterService chatterService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void onCompleted(ApprovalCompletedEvent event) {
        try {
            switch (event.bizType()) {
                case "pur_order" -> orderService.auditOrder(event.bizId());
                case "pur_inbound" -> inboundService.auditInbound(event.bizId());
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
                case "pur_order" -> orderService.onApprovalRejected(event.bizId());
                case "pur_inbound" -> inboundService.onApprovalRejected(event.bizId());
                default -> { return; }
            }
        } catch (Exception e) {
            log.error("[onRejected] 审批驳回回调失败: {} bizId={}", event.bizType(), event.bizId(), e);
        }
    }
}
