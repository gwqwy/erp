package cn.tiangong.module.fin.service;

import cn.tiangong.framework.web.core.exception.ServiceException;
import cn.tiangong.module.bas.api.SerialNoApi;
import cn.tiangong.module.fin.dal.dataobject.ErpFinanceVoucherDO;
import cn.tiangong.module.fin.dal.dataobject.ErpFinanceVoucherItemDO;
import cn.tiangong.module.fin.dal.dataobject.ErpSubjectDO;
import cn.tiangong.module.fin.dal.dataobject.ErpVoucherTemplateDO;
import cn.tiangong.module.fin.dal.mysql.ErpFinanceVoucherItemMapper;
import cn.tiangong.module.fin.dal.mysql.ErpFinanceVoucherMapper;
import cn.tiangong.module.fin.dal.mysql.ErpSubjectMapper;
import cn.tiangong.module.fin.dal.mysql.ErpVoucherTemplateMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static cn.tiangong.framework.web.core.exception.ServiceException.of;

/**
 * 凭证模板引擎（《02-总体架构》§4.3）：按 模板规则 × 单据总额 生成总账凭证。
 * 模板借方合计 ≠ 贷方合计视为配置错误，抛业务异常（调用方决定降级策略）。
 */
@RequiredArgsConstructor
@Service
public class VoucherPostingService {

    public static final int STATUS_POSTED = 2;

    private final ErpVoucherTemplateMapper templateMapper;
    private final ErpSubjectMapper subjectMapper;
    private final ErpFinanceVoucherMapper voucherMapper;
    private final ErpFinanceVoucherItemMapper voucherItemMapper;
    private final SerialNoApi serialNoApi;

    /**
     * 按模板生成并过账凭证。
     *
     * @return 凭证编号；未配置模板/金额非法/借贷不平时抛业务异常
     */
    @Transactional(rollbackFor = Exception.class)
    public String generate(String bizType, String event, Long bizId, String bizNo,
                           Long partnerId, BigDecimal amount, LocalDate voucherDate) {
        List<ErpVoucherTemplateDO> templates = templateMapper.selectList(
                new LambdaQueryWrapper<ErpVoucherTemplateDO>()
                        .eq(ErpVoucherTemplateDO::getBizType, bizType)
                        .eq(ErpVoucherTemplateDO::getEvent, event)
                        .eq(ErpVoucherTemplateDO::getEnabled, 1));
        if (templates.isEmpty()) {
            throw of(8_02, 1, String.format("未配置凭证模板: %s@%s", bizType, event));
        }
        if (amount == null || amount.signum() <= 0) {
            throw of(8_02, 2, "凭证金额必须为正数");
        }

        // 按模板构建分录并校验借贷平衡
        List<ErpFinanceVoucherItemDO> items = new ArrayList<>(templates.size());
        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;
        for (ErpVoucherTemplateDO template : templates) {
            ErpSubjectDO subject = subjectMapper.selectById(template.getSubjectId());
            if (subject == null) {
                throw of(8_02, 3, "模板科目不存在: " + template.getSubjectId());
            }
            ErpFinanceVoucherItemDO item = new ErpFinanceVoucherItemDO();
            item.setSubjectId(subject.getId());
            item.setSubjectNo(subject.getNo());
            item.setSummary(template.getSummary() + " " + (bizNo == null ? "" : bizNo));
            item.setPartnerId(partnerId);
            if ("debit".equals(template.getDirection())) {
                item.setDebit(amount);
                item.setCredit(BigDecimal.ZERO);
                totalDebit = totalDebit.add(amount);
            } else {
                item.setDebit(BigDecimal.ZERO);
                item.setCredit(amount);
                totalCredit = totalCredit.add(amount);
            }
            items.add(item);
        }
        if (totalDebit.compareTo(totalCredit) != 0) {
            throw of(8_02, 4, String.format("模板借贷不平: 借 %s / 贷 %s",
                    totalDebit.stripTrailingZeros().toPlainString(),
                    totalCredit.stripTrailingZeros().toPlainString()));
        }

        ErpFinanceVoucherDO voucher = new ErpFinanceVoucherDO();
        voucher.setNo(serialNoApi.nextNo("finance_voucher"));
        voucher.setVoucherDate(voucherDate != null ? voucherDate : LocalDate.now());
        voucher.setPeriod(voucher.getVoucherDate().format(DateTimeFormatter.ofPattern("yyyy-MM")));
        voucher.setBizType(bizType);
        voucher.setBizId(bizId);
        voucher.setSourceNo(bizNo);
        voucher.setTotalDebit(totalDebit);
        voucher.setTotalCredit(totalCredit);
        voucher.setStatus(STATUS_POSTED);
        voucher.setPostedTime(LocalDateTime.now());
        voucherMapper.insert(voucher);

        for (ErpFinanceVoucherItemDO item : items) {
            item.setVoucherId(voucher.getId());
            voucherItemMapper.insert(item);
        }
        return voucher.getNo();
    }

    /**
     * 成本结转等"可失败旁路"凭证：独立 REQUIRES_NEW 事务，
     * 失败只回滚自身，不拖累调用方的应收/收入主事务（监听器须通过代理调用本方法）。
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public String generateIsolated(String bizType, String event, Long bizId, String bizNo,
                                   Long partnerId, BigDecimal amount, LocalDate voucherDate) {
        return generate(bizType, event, bizId, bizNo, partnerId, amount, voucherDate);
    }

    /** 按来源业务找到原凭证，生成借贷互换的红字冲销凭证 */
    @Transactional(rollbackFor = Exception.class)
    public String reverseBySource(String bizType, Long bizId) {
        ErpFinanceVoucherDO original = voucherMapper.selectOne(new LambdaQueryWrapper<ErpFinanceVoucherDO>()
                .eq(ErpFinanceVoucherDO::getBizType, bizType)
                .eq(ErpFinanceVoucherDO::getBizId, bizId).last("LIMIT 1"));
        if (original == null) {
            throw of(8_02, 5, "未找到原凭证，无法冲销");
        }
        List<ErpFinanceVoucherItemDO> originalItems = voucherItemMapper.selectList(
                new LambdaQueryWrapper<ErpFinanceVoucherItemDO>()
                        .eq(ErpFinanceVoucherItemDO::getVoucherId, original.getId()));

        ErpFinanceVoucherDO red = new ErpFinanceVoucherDO();
        red.setNo(serialNoApi.nextNo("finance_voucher_red"));
        red.setVoucherDate(LocalDate.now());
        red.setPeriod(red.getVoucherDate().format(DateTimeFormatter.ofPattern("yyyy-MM")));
        red.setBizType(original.getBizType());
        red.setBizId(original.getBizId());
        red.setSourceNo(original.getSourceNo());
        red.setTotalDebit(original.getTotalCredit());
        red.setTotalCredit(original.getTotalDebit());
        red.setStatus(STATUS_POSTED);
        red.setPostedTime(LocalDateTime.now());
        voucherMapper.insert(red);

        for (ErpFinanceVoucherItemDO item : originalItems) {
            ErpFinanceVoucherItemDO line = new ErpFinanceVoucherItemDO();
            line.setVoucherId(red.getId());
            line.setSubjectId(item.getSubjectId());
            line.setSubjectNo(item.getSubjectNo());
            line.setSummary("红字冲销 " + (item.getSummary() == null ? "" : item.getSummary()));
            line.setPartnerId(item.getPartnerId());
            line.setDebit(item.getCredit());
            line.setCredit(item.getDebit());
            voucherItemMapper.insert(line);
        }
        return red.getNo();
    }
}
