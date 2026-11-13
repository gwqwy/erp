package cn.tiangong.module.fin.controller.admin;

import cn.tiangong.framework.web.core.pojo.CommonResult;
import cn.tiangong.framework.web.core.pojo.PageParam;
import cn.tiangong.framework.web.core.pojo.PageResult;
import cn.tiangong.module.bas.api.PartnerApi;
import cn.tiangong.module.fin.dal.dataobject.ErpFinanceAccountDO;
import cn.tiangong.module.fin.dal.dataobject.ErpFinancePayableDO;
import cn.tiangong.module.fin.dal.dataobject.ErpFinanceReceivableDO;
import cn.tiangong.module.fin.dal.dataobject.ErpFinanceVoucherDO;
import cn.tiangong.module.fin.dal.dataobject.ErpFinanceVoucherItemDO;
import cn.tiangong.module.fin.dal.dataobject.ErpSubjectDO;
import cn.tiangong.module.fin.dal.dataobject.ErpVoucherTemplateDO;
import cn.tiangong.module.fin.dal.mysql.ErpFinanceAccountMapper;
import cn.tiangong.module.fin.dal.mysql.ErpFinancePayableMapper;
import cn.tiangong.module.fin.dal.mysql.ErpFinanceReceivableMapper;
import cn.tiangong.module.fin.dal.mysql.ErpFinanceVoucherItemMapper;
import cn.tiangong.module.fin.dal.mysql.ErpFinanceVoucherMapper;
import cn.tiangong.module.fin.dal.mysql.ErpSubjectMapper;
import cn.tiangong.module.fin.dal.mysql.ErpVoucherTemplateMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import java.math.BigDecimal;

/**
 * 财务查询与配置：应收/应付台账、凭证、科目、账户、凭证模板。
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/admin-api/fin")
public class FinanceQueryController {

    private final ErpFinanceReceivableMapper receivableMapper;
    private final ErpFinancePayableMapper payableMapper;
    private final ErpFinanceVoucherMapper voucherMapper;
    private final ErpFinanceVoucherItemMapper voucherItemMapper;
    private final ErpSubjectMapper subjectMapper;
    private final ErpFinanceAccountMapper accountMapper;
    private final ErpVoucherTemplateMapper templateMapper;
    private final PartnerApi partnerApi;

    // ========== 台账 ==========

    @GetMapping("/receivable/page")
    @PreAuthorize("@ss.hasPermission('fin:receivable:query')")
    public CommonResult<PageResult<ReceivableRespVO>> getReceivablePage(PageParam pageParam,
                                                                        @RequestParam(required = false) Long partnerId,
                                                                        @RequestParam(required = false) Integer status) {
        LambdaQueryWrapper<ErpFinanceReceivableDO> wrapper = new LambdaQueryWrapper<ErpFinanceReceivableDO>()
                .eq(partnerId != null, ErpFinanceReceivableDO::getPartnerId, partnerId)
                .eq(status != null, ErpFinanceReceivableDO::getStatus, status)
                .orderByDesc(ErpFinanceReceivableDO::getId);
        PageResult<ErpFinanceReceivableDO> page = receivableMapper.selectPage(pageParam, wrapper);
        Map<Long, String> names = partnerApi.getPartnerNames(
                page.getList().stream().map(ErpFinanceReceivableDO::getPartnerId).collect(Collectors.toSet()));
        return CommonResult.success(PageResult.of(page, r -> ReceivableRespVO.of(r, names)));
    }

    @GetMapping("/payable/page")
    @PreAuthorize("@ss.hasPermission('fin:payable:query')")
    public CommonResult<PageResult<PayableRespVO>> getPayablePage(PageParam pageParam,
                                                                  @RequestParam(required = false) Long partnerId,
                                                                  @RequestParam(required = false) Integer status) {
        LambdaQueryWrapper<ErpFinancePayableDO> wrapper = new LambdaQueryWrapper<ErpFinancePayableDO>()
                .eq(partnerId != null, ErpFinancePayableDO::getPartnerId, partnerId)
                .eq(status != null, ErpFinancePayableDO::getStatus, status)
                .orderByDesc(ErpFinancePayableDO::getId);
        PageResult<ErpFinancePayableDO> page = payableMapper.selectPage(pageParam, wrapper);
        Map<Long, String> names = partnerApi.getPartnerNames(
                page.getList().stream().map(ErpFinancePayableDO::getPartnerId).collect(Collectors.toSet()));
        return CommonResult.success(PageResult.of(page, p -> PayableRespVO.of(p, names)));
    }

    // ========== 凭证 ==========

    @GetMapping("/voucher/page")
    @PreAuthorize("@ss.hasPermission('fin:voucher:query')")
    public CommonResult<PageResult<ErpFinanceVoucherDO>> getVoucherPage(PageParam pageParam,
                                                                        @RequestParam(required = false) String no,
                                                                        @RequestParam(required = false) String bizType) {
        LambdaQueryWrapper<ErpFinanceVoucherDO> wrapper = new LambdaQueryWrapper<ErpFinanceVoucherDO>()
                .like(no != null && !no.isBlank(), ErpFinanceVoucherDO::getNo, no)
                .eq(bizType != null && !bizType.isBlank(), ErpFinanceVoucherDO::getBizType, bizType)
                .orderByDesc(ErpFinanceVoucherDO::getId);
        return CommonResult.success(voucherMapper.selectPage(pageParam, wrapper));
    }

    @GetMapping("/voucher/detail")
    @PreAuthorize("@ss.hasPermission('fin:voucher:query')")
    public CommonResult<VoucherDetailRespVO> getVoucherDetail(@RequestParam("id") Long id) {
        ErpFinanceVoucherDO voucher = voucherMapper.selectById(id);
        if (voucher == null) {
            return CommonResult.error(404, "凭证不存在");
        }
        List<ErpFinanceVoucherItemDO> items = voucherItemMapper.selectList(
                new LambdaQueryWrapper<ErpFinanceVoucherItemDO>()
                        .eq(ErpFinanceVoucherItemDO::getVoucherId, id)
                        .orderByAsc(ErpFinanceVoucherItemDO::getId));
        return CommonResult.success(new VoucherDetailRespVO(voucher, items));
    }

    // ========== 科目 / 账户 ==========

    @GetMapping("/subject/list")
    public CommonResult<List<ErpSubjectDO>> getSubjectList() {
        return CommonResult.success(subjectMapper.selectList(
                new LambdaQueryWrapper<ErpSubjectDO>().orderByAsc(ErpSubjectDO::getNo)));
    }

    @GetMapping("/account/list")
    public CommonResult<List<ErpFinanceAccountDO>> getAccountList() {
        return CommonResult.success(accountMapper.selectList(
                new LambdaQueryWrapper<ErpFinanceAccountDO>()
                        .eq(ErpFinanceAccountDO::getStatus, 0)
                        .orderByAsc(ErpFinanceAccountDO::getId)));
    }

    // ========== 凭证模板 CRUD ==========

    @GetMapping("/template/page")
    @PreAuthorize("@ss.hasPermission('fin:template:query')")
    public CommonResult<PageResult<ErpVoucherTemplateDO>> getTemplatePage(PageParam pageParam,
                                                                          @RequestParam(required = false) String bizType) {
        LambdaQueryWrapper<ErpVoucherTemplateDO> wrapper = new LambdaQueryWrapper<ErpVoucherTemplateDO>()
                .like(bizType != null && !bizType.isBlank(), ErpVoucherTemplateDO::getBizType, bizType)
                .orderByAsc(ErpVoucherTemplateDO::getBizType);
        return CommonResult.success(templateMapper.selectPage(pageParam, wrapper));
    }

    @PostMapping("/template/create")
    @PreAuthorize("@ss.hasPermission('fin:template:create')")
    public CommonResult<Long> createTemplate(@RequestParam("bizType") @NotBlank String bizType,
                                             @RequestParam("event") @NotBlank String event,
                                             @RequestParam("subjectId") @NotNull Long subjectId,
                                             @RequestParam("direction") @NotBlank String direction,
                                             @RequestParam("summary") String summary) {
        ErpVoucherTemplateDO template = new ErpVoucherTemplateDO();
        template.setBizType(bizType);
        template.setEvent(event);
        template.setSubjectId(subjectId);
        template.setDirection(direction);
        template.setSummary(summary);
        template.setAmountSource("TOTAL");
        template.setEnabled(1);
        templateMapper.insert(template);
        return CommonResult.success(template.getId());
    }

    @PutMapping("/template/update")
    @PreAuthorize("@ss.hasPermission('fin:template:update')")
    public CommonResult<Boolean> updateTemplate(@RequestParam("id") Long id,
                                                @RequestParam(value = "subjectId", required = false) Long subjectId,
                                                @RequestParam(value = "direction", required = false) String direction,
                                                @RequestParam(value = "summary", required = false) String summary,
                                                @RequestParam(value = "enabled", required = false) Integer enabled) {
        ErpVoucherTemplateDO template = templateMapper.selectById(id);
        if (template == null) {
            return CommonResult.error(404, "模板不存在");
        }
        if (subjectId != null) template.setSubjectId(subjectId);
        if (direction != null) template.setDirection(direction);
        if (summary != null) template.setSummary(summary);
        if (enabled != null) template.setEnabled(enabled);
        templateMapper.updateById(template);
        return CommonResult.success(true);
    }

    @DeleteMapping("/template/delete")
    @PreAuthorize("@ss.hasPermission('fin:template:delete')")
    public CommonResult<Boolean> deleteTemplate(@RequestParam("id") Long id) {
        templateMapper.deleteById(id);
        return CommonResult.success(true);
    }

    // ========== 财务报表（简版） ==========

    /** 科目余额表：按会计期间汇总已过账凭证分录 */
    @GetMapping("/report/balance")
    @PreAuthorize("@ss.hasPermission('fin:report:query')")
    public CommonResult<List<BalanceLineVO>> getBalanceReport(@RequestParam("period") String period) {
        List<ErpFinanceVoucherDO> vouchers = voucherMapper.selectList(
                new LambdaQueryWrapper<ErpFinanceVoucherDO>()
                        .eq(ErpFinanceVoucherDO::getPeriod, period)
                        .eq(ErpFinanceVoucherDO::getStatus, 2));
        Map<String, BigDecimal[]> agg = new java.util.LinkedHashMap<>();
        for (ErpFinanceVoucherDO v : vouchers) {
            for (ErpFinanceVoucherItemDO i : voucherItemMapper.selectList(
                    new LambdaQueryWrapper<ErpFinanceVoucherItemDO>()
                            .eq(ErpFinanceVoucherItemDO::getVoucherId, v.getId()))) {
                String key = i.getSubjectNo() + " " + subjectName(i.getSubjectId());
                BigDecimal[] row = agg.computeIfAbsent(key,
                        k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
                row[0] = row[0].add(i.getDebit() == null ? BigDecimal.ZERO : i.getDebit());
                row[1] = row[1].add(i.getCredit() == null ? BigDecimal.ZERO : i.getCredit());
            }
        }
        List<BalanceLineVO> lines = agg.entrySet().stream()
                .map(e -> new BalanceLineVO(e.getKey(),
                        e.getValue()[0], e.getValue()[1],
                        e.getValue()[0].subtract(e.getValue()[1])))
                .toList();
        return CommonResult.success(lines);
    }

    /** 利润简表：期间收入（6001 贷方-借方）- 成本（6401 借方-贷方） */
    @GetMapping("/report/profit")
    @PreAuthorize("@ss.hasPermission('fin:report:query')")
    public CommonResult<ProfitVO> getProfitReport(@RequestParam("period") String period) {
        BigDecimal revenue = BigDecimal.ZERO;
        BigDecimal cost = BigDecimal.ZERO;
        List<ErpFinanceVoucherDO> vouchers = voucherMapper.selectList(
                new LambdaQueryWrapper<ErpFinanceVoucherDO>()
                        .eq(ErpFinanceVoucherDO::getPeriod, period)
                        .eq(ErpFinanceVoucherDO::getStatus, 2));
        for (ErpFinanceVoucherDO v : vouchers) {
            for (ErpFinanceVoucherItemDO i : voucherItemMapper.selectList(
                    new LambdaQueryWrapper<ErpFinanceVoucherItemDO>()
                            .eq(ErpFinanceVoucherItemDO::getVoucherId, v.getId()))) {
                ErpSubjectDO subject = subjectMapper.selectById(i.getSubjectId());
                if (subject == null) continue;
                BigDecimal debit = i.getDebit() == null ? BigDecimal.ZERO : i.getDebit();
                BigDecimal credit = i.getCredit() == null ? BigDecimal.ZERO : i.getCredit();
                if ("6001".equals(subject.getNo())) {
                    revenue = revenue.add(credit.subtract(debit));
                } else if ("6401".equals(subject.getNo())) {
                    cost = cost.add(debit.subtract(credit));
                }
            }
        }
        return CommonResult.success(new ProfitVO(period, revenue, cost, revenue.subtract(cost)));
    }

    private String subjectName(Long subjectId) {
        ErpSubjectDO subject = subjectMapper.selectById(subjectId);
        return subject == null ? "" : subject.getName();
    }

    /** 应收账龄：按客户汇总未核销余额 + 最早未核销到期日距今天数 */
    @GetMapping("/report/aging")
    @PreAuthorize("@ss.hasPermission('fin:receivable:query')")
    public CommonResult<List<Map<String, Object>>> getAgingReport() {
        LambdaQueryWrapper<ErpFinanceReceivableDO> wrapper = new LambdaQueryWrapper<ErpFinanceReceivableDO>()
                .in(ErpFinanceReceivableDO::getStatus, List.of(1, 2));
        List<ErpFinanceReceivableDO> list = receivableMapper.selectList(wrapper);
        Map<Long, java.math.BigDecimal[]> totals = new java.util.LinkedHashMap<>();
        Map<Long, java.time.LocalDate> earliest = new java.util.HashMap<>();
        for (ErpFinanceReceivableDO r : list) {
            BigDecimal unreceived = r.getAmount().subtract(r.getReceivedAmount());
            BigDecimal[] row = totals.computeIfAbsent(r.getPartnerId(),
                    k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            row[0] = row[0].add(unreceived);
            row[1] = row[1].add(r.getAmount());
            java.time.LocalDate due = r.getDueDate();
            if (due != null) {
                earliest.merge(r.getPartnerId(), due,
                        (a, b) -> a.isBefore(b) ? a : b);
            }
        }
        Map<Long, String> names = partnerApi.getPartnerNames(totals.keySet());
        List<Map<String, Object>> result = new java.util.ArrayList<>();
        for (Map.Entry<Long, BigDecimal[]> e : totals.entrySet()) {
            java.time.LocalDate due = earliest.get(e.getKey());
            long overdueDays = 0;
            if (due != null && due.isBefore(java.time.LocalDate.now())) {
                overdueDays = java.time.temporal.ChronoUnit.DAYS.between(due, java.time.LocalDate.now());
            }
            Map<String, Object> row = new java.util.LinkedHashMap<>();
            row.put("partnerId", String.valueOf(e.getKey()));
            row.put("partnerName", names.getOrDefault(e.getKey(), "未知客户"));
            row.put("unreceivedAmount", e.getValue()[0]);
            row.put("totalAmount", e.getValue()[1]);
            row.put("earliestDue", due == null ? "" : due.toString());
            row.put("overdueDays", overdueDays);
            result.add(row);
        }
        return CommonResult.success(result);
    }

    public record BalanceLineVO(String subject, java.math.BigDecimal debit,
                                java.math.BigDecimal credit, java.math.BigDecimal balance) {
    }

    public record ProfitVO(String period, java.math.BigDecimal revenue,
                           java.math.BigDecimal cost, java.math.BigDecimal profit) {
    }

    public record ReceivableRespVO(String id, String bizType, String bizId, String bizNo, String partnerId,
                                   String partnerName, java.math.BigDecimal amount,
                                   java.math.BigDecimal receivedAmount,
                                   java.math.BigDecimal unreceivedAmount, Integer status) {
        static ReceivableRespVO of(ErpFinanceReceivableDO r, Map<Long, String> names) {
            return new ReceivableRespVO(String.valueOf(r.getId()), r.getBizType(), String.valueOf(r.getBizId()),
                    r.getBizNo(), String.valueOf(r.getPartnerId()),
                    names.getOrDefault(r.getPartnerId(), "未知单位"), r.getAmount(), r.getReceivedAmount(),
                    r.getAmount().subtract(r.getReceivedAmount()), r.getStatus());
        }
    }

    public record PayableRespVO(String id, String bizType, String bizId, String bizNo, String partnerId,
                                String partnerName, java.math.BigDecimal amount,
                                java.math.BigDecimal paidAmount,
                                java.math.BigDecimal unpaidAmount, Integer status) {
        static PayableRespVO of(ErpFinancePayableDO p, Map<Long, String> names) {
            return new PayableRespVO(String.valueOf(p.getId()), p.getBizType(), String.valueOf(p.getBizId()),
                    p.getBizNo(), String.valueOf(p.getPartnerId()),
                    names.getOrDefault(p.getPartnerId(), "未知单位"), p.getAmount(), p.getPaidAmount(),
                    p.getAmount().subtract(p.getPaidAmount()), p.getStatus());
        }
    }

    public record VoucherDetailRespVO(ErpFinanceVoucherDO voucher, List<ErpFinanceVoucherItemDO> items) {
    }
}
