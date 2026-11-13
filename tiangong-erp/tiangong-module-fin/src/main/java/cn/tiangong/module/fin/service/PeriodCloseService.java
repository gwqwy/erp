package cn.tiangong.module.fin.service;

import cn.tiangong.framework.biz.common.chatter.service.ChatterService;
import cn.tiangong.framework.web.core.exception.ServiceException;
import cn.tiangong.module.bas.api.SerialNoApi;
import cn.tiangong.module.fin.dal.dataobject.ErpFinanceVoucherDO;
import cn.tiangong.module.fin.dal.dataobject.ErpFinanceVoucherItemDO;
import cn.tiangong.module.fin.dal.dataobject.ErpFinancePeriodDO;
import cn.tiangong.module.fin.dal.mysql.ErpFinanceVoucherItemMapper;
import cn.tiangong.module.fin.dal.mysql.ErpFinanceVoucherMapper;
import cn.tiangong.module.fin.dal.mysql.ErpFinancePeriodMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static cn.tiangong.framework.web.core.exception.ServiceException.of;

/**
 * 期末结转：将期间损益（6001/6401/6601）结转至 4103 本年利润，并锁账。
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class PeriodCloseService {

    private final ErpFinanceVoucherMapper voucherMapper;
    private final ErpFinanceVoucherItemMapper voucherItemMapper;
    private final ErpFinancePeriodMapper periodMapper;
    private final SerialNoApi serialNoApi;
    private final ChatterService chatterService;

    public boolean isClosed(String period) {
        ErpFinancePeriodDO p = periodMapper.selectOne(new LambdaQueryWrapper<ErpFinancePeriodDO>()
                .eq(ErpFinancePeriodDO::getPeriod, period));
        return p != null && Boolean.TRUE.equals(p.getClosed());
    }

    public List<ErpFinancePeriodDO> getPeriodList() {
        return periodMapper.selectList(new LambdaQueryWrapper<ErpFinancePeriodDO>()
                .orderByDesc(ErpFinancePeriodDO::getPeriod));
    }

    /** 期末结转 + 锁账：生成结转凭证并把期间置为已结转 */
    @Transactional(rollbackFor = Exception.class)
    public String closePeriod(String period, String operator) {
        if (isClosed(period)) {
            throw of(8_08, 1, "该期间已结转并锁账");
        }
        List<ErpFinanceVoucherDO> vouchers = voucherMapper.selectList(new LambdaQueryWrapper<ErpFinanceVoucherDO>()
                .eq(ErpFinanceVoucherDO::getPeriod, period)
                .eq(ErpFinanceVoucherDO::getStatus, 2));
        BigDecimal revenue = BigDecimal.ZERO;
        BigDecimal cost = BigDecimal.ZERO;
        BigDecimal expense = BigDecimal.ZERO;
        for (ErpFinanceVoucherDO v : vouchers) {
            for (ErpFinanceVoucherItemDO i : voucherItemMapper.selectList(new LambdaQueryWrapper<ErpFinanceVoucherItemDO>()
                    .eq(ErpFinanceVoucherItemDO::getVoucherId, v.getId()))) {
                BigDecimal debit = i.getDebit() == null ? BigDecimal.ZERO : i.getDebit();
                BigDecimal credit = i.getCredit() == null ? BigDecimal.ZERO : i.getCredit();
                if ("6001".equals(i.getSubjectNo())) {
                    revenue = revenue.add(credit.subtract(debit));
                } else if ("6401".equals(i.getSubjectNo())) {
                    cost = cost.add(debit.subtract(credit));
                } else if ("6601".equals(i.getSubjectNo())) {
                    expense = expense.add(debit.subtract(credit));
                }
            }
        }
        BigDecimal profit = revenue.subtract(cost).subtract(expense);

        String no = serialNoApi.nextNo("finance_voucher_close");
        ErpFinanceVoucherDO voucher = new ErpFinanceVoucherDO();
        voucher.setNo(no);
        voucher.setVoucherDate(LocalDate.now());
        voucher.setPeriod(period);
        voucher.setBizType("period_close");
        voucher.setSourceNo(period);
        voucher.setStatus(2);
        voucher.setPostedTime(LocalDateTime.now());
        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;
        voucherMapper.insert(voucher);

        Long vid = voucher.getId();
        if (revenue.signum() > 0) {
            totalDebit = insertLine(vid, 6L, "6001", "结转本期收入 " + period, revenue, BigDecimal.ZERO);
        }
        if (cost.signum() > 0) {
            totalCredit = totalCredit.add(insertLine(vid, 7L, "6401", "结转本期成本 " + period, BigDecimal.ZERO, cost));
        }
        if (expense.signum() > 0) {
            totalCredit = totalCredit.add(insertLine(vid, 10L, "6601", "结转本期费用 " + period, BigDecimal.ZERO, expense));
        }
        if (profit.signum() >= 0) {
            totalCredit = totalCredit.add(insertLine(vid, 11L, "4103", "结转本年利润 " + period, BigDecimal.ZERO, profit));
        } else {
            totalDebit = totalDebit.add(insertLine(vid, 11L, "4103", "结转本年亏损 " + period, profit.abs(), BigDecimal.ZERO));
        }
        voucher.setTotalDebit(totalDebit);
        voucher.setTotalCredit(totalCredit);
        voucherMapper.updateById(voucher);

        ErpFinancePeriodDO periodDO = periodMapper.selectOne(new LambdaQueryWrapper<ErpFinancePeriodDO>()
                .eq(ErpFinancePeriodDO::getPeriod, period));
        if (periodDO == null) {
            periodDO = new ErpFinancePeriodDO();
            periodDO.setPeriod(period);
            periodDO.setClosed(Boolean.TRUE);
            periodDO.setCloseTime(LocalDateTime.now());
            periodDO.setCloseUser(operator);
            periodMapper.insert(periodDO);
        } else {
            periodDO.setClosed(Boolean.TRUE);
            periodDO.setCloseTime(LocalDateTime.now());
            periodDO.setCloseUser(operator);
            periodMapper.updateById(periodDO);
        }
        chatterService.trace("period_close", vid,
                String.format("期间 %s 结转完成，结转凭证 %s", period, no), null);
        return no;
    }

    private BigDecimal insertLine(Long voucherId, Long subjectId, String subjectNo, String summary,
                                  BigDecimal debit, BigDecimal credit) {
        ErpFinanceVoucherItemDO item = new ErpFinanceVoucherItemDO();
        item.setVoucherId(voucherId);
        item.setSubjectId(subjectId);
        item.setSubjectNo(subjectNo);
        item.setSummary(summary);
        item.setDebit(debit);
        item.setCredit(credit);
        voucherItemMapper.insert(item);
        return debit.max(credit);
    }
}
