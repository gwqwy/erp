package cn.tiangong.module.fin.service;

import cn.tiangong.module.fin.api.dto.FinanceEvent;
import cn.tiangong.module.fin.dal.dataobject.ErpFinancePayableDO;
import cn.tiangong.module.fin.dal.dataobject.ErpFinanceReceivableDO;
import cn.tiangong.module.fin.dal.mysql.ErpFinancePayableMapper;
import cn.tiangong.module.fin.dal.mysql.ErpFinanceReceivableMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 应收应付台账：业务单据审核后自动登记，收付款审核时核销。
 */
@RequiredArgsConstructor
@Service
public class FinanceLedgerService {

    public static final int STATUS_OPEN = 1;
    public static final int STATUS_PARTIAL = 2;
    public static final int STATUS_SETTLED = 3;

    private final ErpFinanceReceivableMapper receivableMapper;
    private final ErpFinancePayableMapper payableMapper;

    /** 销售出库 → 应收 */
    public void createReceivable(FinanceEvent event) {
        ErpFinanceReceivableDO ar = new ErpFinanceReceivableDO();
        ar.setBizType(event.bizType());
        ar.setBizId(event.bizId());
        ar.setBizNo(event.bizNo());
        ar.setPartnerId(event.partnerId());
        ar.setAmount(event.amount());
        ar.setReceivedAmount(BigDecimal.ZERO);
        ar.setDueDate(event.bizDate());
        ar.setStatus(STATUS_OPEN);
        receivableMapper.insert(ar);
    }

    /** 采购入库 → 应付 */
    public void createPayable(FinanceEvent event) {
        ErpFinancePayableDO ap = new ErpFinancePayableDO();
        ap.setBizType(event.bizType());
        ap.setBizId(event.bizId());
        ap.setBizNo(event.bizNo());
        ap.setPartnerId(event.partnerId());
        ap.setAmount(event.amount());
        ap.setPaidAmount(BigDecimal.ZERO);
        ap.setDueDate(event.bizDate());
        ap.setStatus(STATUS_OPEN);
        payableMapper.insert(ap);
    }

    /** 核销应收：返回该应收剩余未核销额 */
    public BigDecimal applyReceivable(Long receivableId, BigDecimal settleAmount) {
        ErpFinanceReceivableDO ar = receivableMapper.selectById(receivableId);
        if (ar == null) {
            throw cn.tiangong.framework.web.core.exception.ServiceException.of(8_01, 1, "应收记录不存在: " + receivableId);
        }
        BigDecimal remaining = ar.getAmount().subtract(ar.getReceivedAmount());
        if (settleAmount.compareTo(remaining) > 0) {
            throw cn.tiangong.framework.web.core.exception.ServiceException.of(8_01, 2,
                    String.format("核销金额超过应收剩余额 %s", remaining.stripTrailingZeros().toPlainString()));
        }
        BigDecimal newReceived = ar.getReceivedAmount().add(settleAmount);
        int newStatus = newReceived.compareTo(ar.getAmount()) >= 0 ? STATUS_SETTLED : STATUS_PARTIAL;
        receivableMapper.update(null, new LambdaUpdateWrapper<ErpFinanceReceivableDO>()
                .eq(ErpFinanceReceivableDO::getId, receivableId)
                .set(ErpFinanceReceivableDO::getReceivedAmount, newReceived)
                .set(ErpFinanceReceivableDO::getStatus, newStatus)
                .set(newStatus == STATUS_SETTLED, ErpFinanceReceivableDO::getSettleDate, LocalDate.now()));
        return ar.getAmount().subtract(newReceived);
    }

    /** 核销应付：返回该应付剩余未核销额 */
    public BigDecimal applyPayable(Long payableId, BigDecimal settleAmount) {
        ErpFinancePayableDO ap = payableMapper.selectById(payableId);
        if (ap == null) {
            throw cn.tiangong.framework.web.core.exception.ServiceException.of(8_01, 3, "应付记录不存在: " + payableId);
        }
        BigDecimal remaining = ap.getAmount().subtract(ap.getPaidAmount());
        if (settleAmount.compareTo(remaining) > 0) {
            throw cn.tiangong.framework.web.core.exception.ServiceException.of(8_01, 4,
                    String.format("核销金额超过应付剩余额 %s", remaining.stripTrailingZeros().toPlainString()));
        }
        BigDecimal newPaid = ap.getPaidAmount().add(settleAmount);
        int newStatus = newPaid.compareTo(ap.getAmount()) >= 0 ? STATUS_SETTLED : STATUS_PARTIAL;
        payableMapper.update(null, new LambdaUpdateWrapper<ErpFinancePayableDO>()
                .eq(ErpFinancePayableDO::getId, payableId)
                .set(ErpFinancePayableDO::getPaidAmount, newPaid)
                .set(ErpFinancePayableDO::getStatus, newStatus)
                .set(newStatus == STATUS_SETTLED, ErpFinancePayableDO::getSettleDate, LocalDate.now()));
        return ap.getAmount().subtract(newPaid);
    }

    /** 冲销回退：应收已核销金额减回，状态重算 */
    public void restoreReceivable(Long receivableId, BigDecimal restoreAmount) {
        ErpFinanceReceivableDO ar = receivableMapper.selectById(receivableId);
        if (ar == null) {
            throw cn.tiangong.framework.web.core.exception.ServiceException.of(8_01, 5, "应收记录不存在: " + receivableId);
        }
        BigDecimal newReceived = ar.getReceivedAmount().subtract(restoreAmount).max(BigDecimal.ZERO);
        int newStatus = newReceived.signum() == 0 ? STATUS_OPEN : STATUS_PARTIAL;
        receivableMapper.update(null, new LambdaUpdateWrapper<ErpFinanceReceivableDO>()
                .eq(ErpFinanceReceivableDO::getId, receivableId)
                .set(ErpFinanceReceivableDO::getReceivedAmount, newReceived)
                .set(ErpFinanceReceivableDO::getStatus, newStatus)
                .set(ErpFinanceReceivableDO::getSettleDate, null));
    }

    /** 冲销回退：应付已核销金额减回，状态重算 */
    public void restorePayable(Long payableId, BigDecimal restoreAmount) {
        ErpFinancePayableDO ap = payableMapper.selectById(payableId);
        if (ap == null) {
            throw cn.tiangong.framework.web.core.exception.ServiceException.of(8_01, 6, "应付记录不存在: " + payableId);
        }
        BigDecimal newPaid = ap.getPaidAmount().subtract(restoreAmount).max(BigDecimal.ZERO);
        int newStatus = newPaid.signum() == 0 ? STATUS_OPEN : STATUS_PARTIAL;
        payableMapper.update(null, new LambdaUpdateWrapper<ErpFinancePayableDO>()
                .eq(ErpFinancePayableDO::getId, payableId)
                .set(ErpFinancePayableDO::getPaidAmount, newPaid)
                .set(ErpFinancePayableDO::getStatus, newStatus)
                .set(ErpFinancePayableDO::getSettleDate, null));
    }

    /** 未结清应收分页数据源（收付款单选择明细行用） */
    public java.util.List<ErpFinanceReceivableDO> listOpenReceivables(Long partnerId) {
        return receivableMapper.selectList(new LambdaQueryWrapper<ErpFinanceReceivableDO>()
                .eq(partnerId != null, ErpFinanceReceivableDO::getPartnerId, partnerId)
                .in(ErpFinanceReceivableDO::getStatus, java.util.List.of(STATUS_OPEN, STATUS_PARTIAL))
                .orderByAsc(ErpFinanceReceivableDO::getId));
    }

    public java.util.List<ErpFinancePayableDO> listOpenPayables(Long partnerId) {
        return payableMapper.selectList(new LambdaQueryWrapper<ErpFinancePayableDO>()
                .eq(partnerId != null, ErpFinancePayableDO::getPartnerId, partnerId)
                .in(ErpFinancePayableDO::getStatus, java.util.List.of(STATUS_OPEN, STATUS_PARTIAL))
                .orderByAsc(ErpFinancePayableDO::getId));
    }
}
