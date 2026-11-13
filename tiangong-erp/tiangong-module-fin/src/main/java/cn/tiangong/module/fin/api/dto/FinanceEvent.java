package cn.tiangong.module.fin.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 财务联动事件：业务模块在单据审核事务内发布，fin 在事务提交后消费
 * （@TransactionalEventListener AFTER_COMMIT——业务成功不因财务异常回滚，《02-总体架构》§4.3）。
 *
 * @param bizType        财务业务键（模板匹配用）：sale_out / purchase_in / receipt / payment
 * @param event          事件：audit
 * @param chatterBizType 来源单据的 Chatter bizType（如 sal_outbound），用于回写轨迹
 * @param bizId          来源单据 id
 * @param bizNo          来源单号
 * @param partnerId      往来单位（客户/供应商）
 * @param amount         金额（M2 = 单据价税合计）
 * @param costAmount     结转成本金额（仅销售出库，为空则不生成成本凭证）
 * @param bizDate        业务日期
 */
public record FinanceEvent(
        String bizType,
        String event,
        String chatterBizType,
        Long bizId,
        String bizNo,
        Long partnerId,
        BigDecimal amount,
        BigDecimal costAmount,
        LocalDate bizDate) {
}
