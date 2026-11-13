package cn.tiangong.module.fin.service;

import cn.tiangong.framework.biz.common.chatter.service.ChatterService;
import cn.tiangong.framework.biz.common.order.BizOrderStatus;
import cn.tiangong.framework.biz.common.order.OrderStateChecker;
import cn.tiangong.framework.security.util.SecurityFrameworkUtils;
import cn.tiangong.framework.web.core.pojo.PageParam;
import cn.tiangong.framework.web.core.pojo.PageResult;
import cn.tiangong.module.bas.api.SerialNoApi;
import cn.tiangong.module.fin.dal.dataobject.ErpFinanceAccountDO;
import cn.tiangong.module.fin.dal.dataobject.ErpFinanceReceiptDO;
import cn.tiangong.module.fin.dal.dataobject.ErpFinanceReceiptItemDO;
import cn.tiangong.module.fin.dal.mysql.ErpFinanceAccountMapper;
import cn.tiangong.module.fin.dal.mysql.ErpFinanceReceiptItemMapper;
import cn.tiangong.module.fin.dal.mysql.ErpFinanceReceiptMapper;
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
public class ReceiptServiceImpl implements ReceiptService {

    public static final String BIZ_TYPE = "finance_receipt";
    /** 60 = 已冲销（红字） */
    public static final int STATUS_REVERSED = 60;

    private final ErpFinanceReceiptMapper receiptMapper;
    private final ErpFinanceReceiptItemMapper receiptItemMapper;
    private final ErpFinanceAccountMapper accountMapper;
    private final SerialNoApi serialNoApi;
    private final FinanceLedgerService ledgerService;
    private final VoucherPostingService voucherPostingService;
    private final ChatterService chatterService;

    @Override
    public PageResult<ErpFinanceReceiptDO> getReceiptPage(PageParam pageParam, String no, Long partnerId, Integer status) {
        LambdaQueryWrapper<ErpFinanceReceiptDO> wrapper = new LambdaQueryWrapper<ErpFinanceReceiptDO>()
                .like(no != null && !no.isBlank(), ErpFinanceReceiptDO::getNo, no)
                .eq(partnerId != null, ErpFinanceReceiptDO::getPartnerId, partnerId)
                .eq(status != null, ErpFinanceReceiptDO::getStatus, status)
                .orderByDesc(ErpFinanceReceiptDO::getId);
        return receiptMapper.selectPage(pageParam, wrapper);
    }

    @Override
    public ReceiptDetail getReceiptDetail(Long id) {
        ErpFinanceReceiptDO receipt = getReceipt(id);
        List<ErpFinanceReceiptItemDO> items = receiptItemMapper.selectList(
                new LambdaQueryWrapper<ErpFinanceReceiptItemDO>()
                        .eq(ErpFinanceReceiptItemDO::getReceiptId, id)
                        .orderByAsc(ErpFinanceReceiptItemDO::getId));
        return new ReceiptDetail(receipt, items);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createReceipt(Long partnerId, Long accountId, LocalDate receiptDate,
                              String remark, List<Item> items) {
        validateHead(partnerId, accountId, items);
        BigDecimal total = items.stream().map(Item::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        ErpFinanceReceiptDO receipt = new ErpFinanceReceiptDO();
        receipt.setNo(serialNoApi.nextNo("finance_receipt"));
        receipt.setPartnerId(partnerId);
        receipt.setAccountId(accountId);
        receipt.setReceiptDate(receiptDate != null ? receiptDate : LocalDate.now());
        receipt.setAmount(total);
        receipt.setUnallocatedAmount(BigDecimal.ZERO);
        receipt.setRemark(remark);
        receipt.setStatus(BizOrderStatus.DRAFT.getValue());
        receiptMapper.insert(receipt);
        for (Item item : items) {
            ErpFinanceReceiptItemDO itemDO = new ErpFinanceReceiptItemDO();
            itemDO.setReceiptId(receipt.getId());
            itemDO.setReceivableId(item.receivableId());
            itemDO.setAmount(item.amount());
            itemDO.setRemark(item.remark());
            receiptItemMapper.insert(itemDO);
        }
        chatterService.trace(BIZ_TYPE, receipt.getId(), "创建收款单 " + receipt.getNo(), null);
        return receipt.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteReceipt(Long id) {
        ErpFinanceReceiptDO receipt = getReceipt(id);
        OrderStateChecker.require(receipt.getStatus(), receipt.getNo(), BizOrderStatus.DRAFT);
        receiptMapper.deleteById(id);
        receiptItemMapper.delete(new LambdaQueryWrapper<ErpFinanceReceiptItemDO>()
                .eq(ErpFinanceReceiptItemDO::getReceiptId, id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditReceipt(Long id) {
        ErpFinanceReceiptDO receipt = getReceipt(id);
        OrderStateChecker.require(receipt.getStatus(), receipt.getNo(), BizOrderStatus.DRAFT);
        List<ErpFinanceReceiptItemDO> items = receiptItemMapper.selectList(
                new LambdaQueryWrapper<ErpFinanceReceiptItemDO>()
                        .eq(ErpFinanceReceiptItemDO::getReceiptId, id));
        if (items.isEmpty()) {
            throw of(8_03, 1, "核销明细不能为空");
        }

        // 1. 核销应收台账
        for (ErpFinanceReceiptItemDO item : items) {
            ledgerService.applyReceivable(item.getReceivableId(), item.getAmount());
        }

        // 2. 自动凭证（模板 receipt@audit）
        String voucherNo = voucherPostingService.generate("receipt", "audit", receipt.getId(),
                receipt.getNo(), receipt.getPartnerId(), receipt.getAmount(), receipt.getReceiptDate());

        // 3. 结算账户余额增加
        ErpFinanceAccountDO account = accountMapper.selectById(receipt.getAccountId());
        if (account == null) {
            throw of(8_03, 2, "结算账户不存在");
        }
        accountMapper.update(null, new LambdaUpdateWrapper<ErpFinanceAccountDO>()
                .eq(ErpFinanceAccountDO::getId, account.getId())
                .set(ErpFinanceAccountDO::getBalance,
                        account.getBalance().add(receipt.getAmount())));

        receipt.setStatus(BizOrderStatus.AUDITED.getValue());
        receipt.setAuditUserId(SecurityFrameworkUtils.getLoginUserId());
        receipt.setAuditTime(LocalDateTime.now());
        receiptMapper.updateById(receipt);
        chatterService.trace(BIZ_TYPE, id,
                String.format("审核收款单 %s，核销应收 %s，凭证 %s",
                        receipt.getNo(),
                        receipt.getAmount().stripTrailingZeros().toPlainString(), voucherNo), null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void voidReceipt(Long id) {
        ErpFinanceReceiptDO receipt = getReceipt(id);
        OrderStateChecker.require(receipt.getStatus(), receipt.getNo(), BizOrderStatus.DRAFT);
        receipt.setStatus(BizOrderStatus.VOIDED.getValue());
        receiptMapper.updateById(receipt);
        chatterService.trace(BIZ_TYPE, id, "作废收款单 " + receipt.getNo(), null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reverseReceipt(Long id) {
        ErpFinanceReceiptDO receipt = getReceipt(id);
        OrderStateChecker.require(receipt.getStatus(), receipt.getNo(), BizOrderStatus.AUDITED);
        List<ErpFinanceReceiptItemDO> items = receiptItemMapper.selectList(
                new LambdaQueryWrapper<ErpFinanceReceiptItemDO>()
                        .eq(ErpFinanceReceiptItemDO::getReceiptId, id));

        // 1. 退回核销：应收已核销金额回退、状态重算
        for (ErpFinanceReceiptItemDO item : items) {
            ledgerService.restoreReceivable(item.getReceivableId(), item.getAmount());
        }

        // 2. 红字凭证（借贷互换）
        String redVoucherNo = voucherPostingService.reverseBySource(BIZ_TYPE, id);

        // 3. 结算账户余额扣回
        ErpFinanceAccountDO account = accountMapper.selectById(receipt.getAccountId());
        if (account != null) {
            accountMapper.update(null, new LambdaUpdateWrapper<ErpFinanceAccountDO>()
                    .eq(ErpFinanceAccountDO::getId, account.getId())
                    .set(ErpFinanceAccountDO::getBalance,
                            account.getBalance().subtract(receipt.getAmount())));
        }

        receipt.setStatus(STATUS_REVERSED);
        receiptMapper.updateById(receipt);
        chatterService.trace(BIZ_TYPE, id,
                String.format("红字冲销收款单 %s，核销已退回，红字凭证 %s", receipt.getNo(), redVoucherNo), null);
    }

    private void validateHead(Long partnerId, Long accountId, List<Item> items) {
        if (partnerId == null || accountId == null) {
            throw of(8_03, 3, "往来单位与结算账户不能为空");
        }
        if (items == null || items.isEmpty()) {
            throw of(8_03, 4, "核销明细不能为空");
        }
        for (Item item : items) {
            if (item.receivableId() == null || item.amount() == null || item.amount().signum() <= 0) {
                throw of(8_03, 5, "明细中存在应收记录/金额不合法的行");
            }
        }
    }

    private ErpFinanceReceiptDO getReceipt(Long id) {
        ErpFinanceReceiptDO receipt = receiptMapper.selectById(id);
        if (receipt == null) {
            throw of(8_03, 6, "收款单不存在");
        }
        return receipt;
    }
}
