package cn.tiangong.module.fin.service;

import cn.tiangong.framework.biz.common.chatter.service.ChatterService;
import cn.tiangong.module.fin.api.dto.FinanceEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 财务联动监听器：业务事务提交后执行（业务成功不因财务异常回滚）。
 * 失败时在来源单据 Chatter 记录失败轨迹，财务可后续手工补录。
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class FinanceEventListener {

    private final FinanceLedgerService ledgerService;
    private final VoucherPostingService voucherPostingService;
    private final ChatterService chatterService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void onFinanceEvent(FinanceEvent event) {
        try {
            String voucherNo;
            switch (event.bizType()) {
                case "sale_return", "purchase_return", "stock_check_gain", "stock_check_loss" -> {
                    // 退货冲减 / 盘点盈亏：只生成凭证，不动应收应付台账
                    voucherNo = voucherPostingService.generate(event.bizType(), event.event(),
                            event.bizId(), event.bizNo(), event.partnerId(), event.amount(), event.bizDate());
                }
                case "sale_out" -> {
                    ledgerService.createReceivable(event);
                    voucherNo = voucherPostingService.generate(event.bizType(), event.event(),
                            event.bizId(), event.bizNo(), event.partnerId(), event.amount(), event.bizDate());
                    // 销售成本结转（借 6401 / 贷 1405）：独立事务，模板缺失不影响应收主流程
                    if (event.costAmount() != null && event.costAmount().signum() > 0) {
                        try {
                            String costVoucher = voucherPostingService.generateIsolated("sale_out_cost", "audit",
                                    event.bizId(), event.bizNo(), null, event.costAmount(), event.bizDate());
                            chatterService.trace(event.chatterBizType(), event.bizId(),
                                    "成本结转凭证 " + costVoucher, null);
                        } catch (Exception costEx) {
                            log.warn("[onFinanceEvent] 成本结转凭证生成失败（不影响应收）: bizId={}, {}",
                                    event.bizId(), costEx.getMessage());
                            chatterService.trace(event.chatterBizType(), event.bizId(),
                                    "成本结转凭证生成失败：" + costEx.getMessage(), null);
                        }
                    }
                }
                case "purchase_in" -> {
                    ledgerService.createPayable(event);
                    voucherNo = voucherPostingService.generate(event.bizType(), event.event(),
                            event.bizId(), event.bizNo(), event.partnerId(), event.amount(), event.bizDate());
                }
                default -> throw new IllegalArgumentException("未知的财务事件类型: " + event.bizType());
            }
            String action = "sale_out".equals(event.bizType()) ? "生成应收与凭证" : "生成应付与凭证";
            chatterService.trace(event.chatterBizType(), event.bizId(),
                    String.format("财务联动：%s %s", action, voucherNo), null);
        } catch (Exception e) {
            log.error("[onFinanceEvent] 财务联动失败: {}@{} bizId={}", event.bizType(), event.event(),
                    event.bizId(), e);
            try {
                chatterService.trace(event.chatterBizType(), event.bizId(),
                        "财务联动失败：" + e.getMessage() + "（业务不受影响，请手工补录）", null);
            } catch (Exception traceEx) {
                log.error("[onFinanceEvent] 失败轨迹记录异常", traceEx);
            }
        }
    }
}
