package cn.tiangong.module.fin.service;

import cn.tiangong.framework.biz.common.chatter.service.ChatterService;
import cn.tiangong.framework.biz.common.order.BizOrderStatus;
import cn.tiangong.framework.biz.common.order.OrderStateChecker;
import cn.tiangong.framework.security.util.SecurityFrameworkUtils;
import cn.tiangong.framework.web.core.pojo.PageParam;
import cn.tiangong.framework.web.core.pojo.PageResult;
import cn.tiangong.module.bas.api.SerialNoApi;
import cn.tiangong.module.fin.dal.dataobject.ErpFinanceAccountDO;
import cn.tiangong.module.fin.dal.dataobject.ErpFinanceExpenseDO;
import cn.tiangong.module.fin.dal.mysql.ErpFinanceAccountMapper;
import cn.tiangong.module.fin.dal.mysql.ErpFinanceExpenseMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static cn.tiangong.framework.web.core.exception.ServiceException.of;

/**
 * 费用登记：审核后生成凭证（借 6601 管理费用 / 贷 1001 现金）+ 账户扣款。
 */
@RequiredArgsConstructor
@Service
public class ExpenseService {

    public static final String BIZ_TYPE = "finance_expense";

    private final ErpFinanceExpenseMapper expenseMapper;
    private final ErpFinanceAccountMapper accountMapper;
    private final SerialNoApi serialNoApi;
    private final VoucherPostingService voucherPostingService;
    private final ChatterService chatterService;

    public PageResult<ErpFinanceExpenseDO> getExpensePage(PageParam pageParam, String no, String category, Integer status) {
        return expenseMapper.selectPage(pageParam, new LambdaQueryWrapper<ErpFinanceExpenseDO>()
                .like(no != null && !no.isBlank(), ErpFinanceExpenseDO::getNo, no)
                .eq(category != null && !category.isBlank(), ErpFinanceExpenseDO::getCategory, category)
                .eq(status != null, ErpFinanceExpenseDO::getStatus, status)
                .orderByDesc(ErpFinanceExpenseDO::getId));
    }

    @Transactional(rollbackFor = Exception.class)
    public Long createExpense(String category, BigDecimal amount, Long accountId,
                              LocalDate expenseDate, String remark) {
        if (category == null || category.isBlank() || amount == null || amount.signum() <= 0 || accountId == null) {
            throw of(8_06, 1, "分类/金额/账户不合法");
        }
        ErpFinanceExpenseDO expense = new ErpFinanceExpenseDO();
        expense.setNo(serialNoApi.nextNo("finance_expense"));
        expense.setCategory(category);
        expense.setAmount(amount);
        expense.setAccountId(accountId);
        expense.setExpenseDate(expenseDate != null ? expenseDate : LocalDate.now());
        expense.setRemark(remark);
        expense.setStatus(BizOrderStatus.DRAFT.getValue());
        expenseMapper.insert(expense);
        return expense.getId();
    }

    /** 审核：生成凭证 + 现金账户扣款 */
    @Transactional(rollbackFor = Exception.class)
    public void auditExpense(Long id) {
        ErpFinanceExpenseDO expense = expenseMapper.selectById(id);
        if (expense == null) {
            throw of(8_06, 2, "费用单不存在");
        }
        OrderStateChecker.require(expense.getStatus(), expense.getNo(), BizOrderStatus.DRAFT);
        String voucherNo = voucherPostingService.generate(BIZ_TYPE, "audit", expense.getId(),
                expense.getNo(), null, expense.getAmount(), expense.getExpenseDate());
        ErpFinanceAccountDO account = accountMapper.selectById(expense.getAccountId());
        if (account != null) {
            accountMapper.update(null, new LambdaUpdateWrapper<ErpFinanceAccountDO>()
                    .eq(ErpFinanceAccountDO::getId, account.getId())
                    .set(ErpFinanceAccountDO::getBalance,
                            account.getBalance().subtract(expense.getAmount())));
        }
        expense.setStatus(BizOrderStatus.AUDITED.getValue());
        expense.setAuditUserId(SecurityFrameworkUtils.getLoginUserId());
        expense.setAuditTime(LocalDateTime.now());
        expenseMapper.updateById(expense);
        chatterService.trace(BIZ_TYPE, id, String.format("审核费用单 %s，凭证 %s", expense.getNo(), voucherNo), null);
    }

    @Transactional(rollbackFor = Exception.class)
    public void voidExpense(Long id) {
        ErpFinanceExpenseDO expense = expenseMapper.selectById(id);
        if (expense == null) {
            throw of(8_06, 2, "费用单不存在");
        }
        OrderStateChecker.require(expense.getStatus(), expense.getNo(), BizOrderStatus.DRAFT);
        expense.setStatus(BizOrderStatus.VOIDED.getValue());
        expenseMapper.updateById(expense);
    }

    public void deleteExpense(Long id) {
        ErpFinanceExpenseDO expense = expenseMapper.selectById(id);
        if (expense == null) {
            throw of(8_06, 2, "费用单不存在");
        }
        OrderStateChecker.require(expense.getStatus(), expense.getNo(), BizOrderStatus.DRAFT);
        expenseMapper.deleteById(id);
    }
}
