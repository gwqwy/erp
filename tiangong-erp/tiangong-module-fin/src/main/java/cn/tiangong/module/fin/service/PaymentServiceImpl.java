package cn.tiangong.module.fin.service;

import cn.tiangong.framework.biz.common.chatter.service.ChatterService;
import cn.tiangong.framework.biz.common.order.BizOrderStatus;
import cn.tiangong.framework.biz.common.order.OrderStateChecker;
import cn.tiangong.framework.security.util.SecurityFrameworkUtils;
import cn.tiangong.framework.web.core.pojo.PageParam;
import cn.tiangong.framework.web.core.pojo.PageResult;
import cn.tiangong.module.bas.api.SerialNoApi;
import cn.tiangong.module.fin.dal.dataobject.ErpFinanceAccountDO;
import cn.tiangong.module.fin.dal.dataobject.ErpFinancePaymentDO;
import cn.tiangong.module.fin.dal.dataobject.ErpFinancePaymentItemDO;
import cn.tiangong.module.fin.dal.mysql.ErpFinanceAccountMapper;
import cn.tiangong.module.fin.dal.mysql.ErpFinancePaymentItemMapper;
import cn.tiangong.module.fin.dal.mysql.ErpFinancePaymentMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static cn.tiangong.framework.web.core.exception.ServiceException.of;

@RequiredArgsConstructor
@Service
public class PaymentServiceImpl implements PaymentService {

    public static final String BIZ_TYPE = "finance_payment";
    /** 60 = 已冲销（红字） */
    public static final int STATUS_REVERSED = 60;

    private final ErpFinancePaymentMapper paymentMapper;
    private final ErpFinancePaymentItemMapper paymentItemMapper;
    private final ErpFinanceAccountMapper accountMapper;
    private final SerialNoApi serialNoApi;
    private final FinanceLedgerService ledgerService;
    private final VoucherPostingService voucherPostingService;
    private final ChatterService chatterService;

    @Override
    public PageResult<ErpFinancePaymentDO> getPaymentPage(PageParam pageParam, String no, Long partnerId, Integer status) {
        LambdaQueryWrapper<ErpFinancePaymentDO> wrapper = new LambdaQueryWrapper<ErpFinancePaymentDO>()
                .like(no != null && !no.isBlank(), ErpFinancePaymentDO::getNo, no)
                .eq(partnerId != null, ErpFinancePaymentDO::getPartnerId, partnerId)
                .eq(status != null, ErpFinancePaymentDO::getStatus, status)
                .orderByDesc(ErpFinancePaymentDO::getId);
        return paymentMapper.selectPage(pageParam, wrapper);
    }

    @Override
    public PaymentDetail getPaymentDetail(Long id) {
        ErpFinancePaymentDO payment = getPayment(id);
        List<ErpFinancePaymentItemDO> items = paymentItemMapper.selectList(
                new LambdaQueryWrapper<ErpFinancePaymentItemDO>()
                        .eq(ErpFinancePaymentItemDO::getPaymentId, id)
                        .orderByAsc(ErpFinancePaymentItemDO::getId));
        return new PaymentDetail(payment, items);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createPayment(Long partnerId, Long accountId, LocalDate paymentDate,
                              String remark, List<Item> items) {
        validateHead(partnerId, accountId, items);
        BigDecimal total = items.stream().map(Item::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        ErpFinancePaymentDO payment = new ErpFinancePaymentDO();
        payment.setNo(serialNoApi.nextNo("finance_payment"));
        payment.setPartnerId(partnerId);
        payment.setAccountId(accountId);
        payment.setPaymentDate(paymentDate != null ? paymentDate : LocalDate.now());
        payment.setAmount(total);
        payment.setUnallocatedAmount(BigDecimal.ZERO);
        payment.setRemark(remark);
        payment.setStatus(BizOrderStatus.DRAFT.getValue());
        paymentMapper.insert(payment);
        for (Item item : items) {
            ErpFinancePaymentItemDO itemDO = new ErpFinancePaymentItemDO();
            itemDO.setPaymentId(payment.getId());
            itemDO.setPayableId(item.payableId());
            itemDO.setAmount(item.amount());
            itemDO.setRemark(item.remark());
            paymentItemMapper.insert(itemDO);
        }
        chatterService.trace(BIZ_TYPE, payment.getId(), "创建付款单 " + payment.getNo(), null);
        return payment.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePayment(Long id) {
        ErpFinancePaymentDO payment = getPayment(id);
        OrderStateChecker.require(payment.getStatus(), payment.getNo(), BizOrderStatus.DRAFT);
        paymentMapper.deleteById(id);
        paymentItemMapper.delete(new LambdaQueryWrapper<ErpFinancePaymentItemDO>()
                .eq(ErpFinancePaymentItemDO::getPaymentId, id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditPayment(Long id) {
        ErpFinancePaymentDO payment = getPayment(id);
        OrderStateChecker.require(payment.getStatus(), payment.getNo(), BizOrderStatus.DRAFT);
        List<ErpFinancePaymentItemDO> items = paymentItemMapper.selectList(
                new LambdaQueryWrapper<ErpFinancePaymentItemDO>()
                        .eq(ErpFinancePaymentItemDO::getPaymentId, id));
        if (items.isEmpty()) {
            throw of(8_04, 1, "核销明细不能为空");
        }

        for (ErpFinancePaymentItemDO item : items) {
            ledgerService.applyPayable(item.getPayableId(), item.getAmount());
        }

        String voucherNo = voucherPostingService.generate("payment", "audit", payment.getId(),
                payment.getNo(), payment.getPartnerId(), payment.getAmount(), payment.getPaymentDate());

        ErpFinanceAccountDO account = accountMapper.selectById(payment.getAccountId());
        if (account == null) {
            throw of(8_04, 2, "结算账户不存在");
        }
        accountMapper.update(null, new LambdaUpdateWrapper<ErpFinanceAccountDO>()
                .eq(ErpFinanceAccountDO::getId, account.getId())
                .set(ErpFinanceAccountDO::getBalance,
                        account.getBalance().subtract(payment.getAmount())));

        payment.setStatus(BizOrderStatus.AUDITED.getValue());
        payment.setAuditUserId(SecurityFrameworkUtils.getLoginUserId());
        payment.setAuditTime(LocalDateTime.now());
        paymentMapper.updateById(payment);
        chatterService.trace(BIZ_TYPE, id,
                String.format("审核付款单 %s，核销应付 %s，凭证 %s",
                        payment.getNo(),
                        payment.getAmount().stripTrailingZeros().toPlainString(), voucherNo), null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void voidPayment(Long id) {
        ErpFinancePaymentDO payment = getPayment(id);
        OrderStateChecker.require(payment.getStatus(), payment.getNo(), BizOrderStatus.DRAFT);
        payment.setStatus(BizOrderStatus.VOIDED.getValue());
        paymentMapper.updateById(payment);
        chatterService.trace(BIZ_TYPE, id, "作废付款单 " + payment.getNo(), null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reversePayment(Long id) {
        ErpFinancePaymentDO payment = getPayment(id);
        OrderStateChecker.require(payment.getStatus(), payment.getNo(), BizOrderStatus.AUDITED);
        List<ErpFinancePaymentItemDO> items = paymentItemMapper.selectList(
                new LambdaQueryWrapper<ErpFinancePaymentItemDO>()
                        .eq(ErpFinancePaymentItemDO::getPaymentId, id));

        // 1. 退回核销：应付已核销金额回退
        for (ErpFinancePaymentItemDO item : items) {
            ledgerService.restorePayable(item.getPayableId(), item.getAmount());
        }

        // 2. 红字凭证（借贷互换）
        String redVoucherNo = voucherPostingService.reverseBySource(BIZ_TYPE, id);

        // 3. 结算账户余额加回
        ErpFinanceAccountDO account = accountMapper.selectById(payment.getAccountId());
        if (account != null) {
            accountMapper.update(null, new LambdaUpdateWrapper<ErpFinanceAccountDO>()
                    .eq(ErpFinanceAccountDO::getId, account.getId())
                    .set(ErpFinanceAccountDO::getBalance,
                            account.getBalance().add(payment.getAmount())));
        }

        payment.setStatus(STATUS_REVERSED);
        paymentMapper.updateById(payment);
        chatterService.trace(BIZ_TYPE, id,
                String.format("红字冲销付款单 %s，核销已退回，红字凭证 %s", payment.getNo(), redVoucherNo), null);
    }

    private void validateHead(Long partnerId, Long accountId, List<Item> items) {
        if (partnerId == null || accountId == null) {
            throw of(8_04, 3, "往来单位与结算账户不能为空");
        }
        if (items == null || items.isEmpty()) {
            throw of(8_04, 4, "核销明细不能为空");
        }
        for (Item item : items) {
            if (item.payableId() == null || item.amount() == null || item.amount().signum() <= 0) {
                throw of(8_04, 5, "明细中存在应付记录/金额不合法的行");
            }
        }
    }

    private ErpFinancePaymentDO getPayment(Long id) {
        ErpFinancePaymentDO payment = paymentMapper.selectById(id);
        if (payment == null) {
            throw of(8_04, 6, "付款单不存在");
        }
        return payment;
    }
}
