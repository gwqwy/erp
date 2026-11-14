package cn.tiangong.module.rpt.controller.admin;

import cn.tiangong.framework.web.core.pojo.CommonResult;
import cn.tiangong.module.rpt.dal.mysql.RptAnalysisMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 经营分析报表：销售/采购排行、库存 ABC、客户对账单。
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/admin-api/rpt")
public class RptAnalysisController {

    private final RptAnalysisMapper analysisMapper;

    private static final DateTimeFormatter D = DateTimeFormatter.ISO_DATE;

    /** 销售商品排行（近 N 天默认全年） */
    @GetMapping("/sale-rank")
    @PreAuthorize("@ss.hasPermission('rpt:rank:query')")
    public CommonResult<List<Map<String, Object>>> saleRank(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(defaultValue = "10") int limit) {
        String[] range = range(from, to);
        return CommonResult.success(analysisMapper.saleRank(range[0], range[1], Math.min(Math.max(limit, 1), 100)));
    }

    /** 采购商品排行 */
    @GetMapping("/purchase-rank")
    @PreAuthorize("@ss.hasPermission('rpt:rank:query')")
    public CommonResult<List<Map<String, Object>>> purchaseRank(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(defaultValue = "10") int limit) {
        String[] range = range(from, to);
        return CommonResult.success(analysisMapper.purchaseRank(range[0], range[1], Math.min(Math.max(limit, 1), 100)));
    }

    /** 库存 ABC 分析（按占用金额：累计 80% A / 80~95% B / 其余 C） */
    @GetMapping("/stock-abc")
    @PreAuthorize("@ss.hasPermission('rpt:abc:query')")
    public CommonResult<List<Map<String, Object>>> stockAbc() {
        List<Map<String, Object>> base = analysisMapper.stockAbcBase();
        BigDecimal total = base.stream()
                .map(m -> new BigDecimal(String.valueOf(m.get("amount"))))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        List<Map<String, Object>> result = new ArrayList<>();
        BigDecimal cumulative = BigDecimal.ZERO;
        for (Map<String, Object> row : base) {
            BigDecimal amount = new BigDecimal(String.valueOf(row.get("amount")));
            cumulative = cumulative.add(amount);
            double ratio = total.signum() == 0 ? 0 : cumulative.divide(total, 4, RoundingMode.HALF_UP).doubleValue();
            String abc = ratio <= 0.80 ? "A" : ratio <= 0.95 ? "B" : "C";
            Map<String, Object> out = new HashMap<>(row);
            out.put("amountRatio", total.signum() == 0 ? 0
                    : amount.divide(total, 4, RoundingMode.HALF_UP).doubleValue());
            out.put("cumulativeRatio", ratio);
            out.put("abcClass", abc);
            result.add(out);
        }
        return CommonResult.success(result);
    }

    /** 客户对账单：期初 + 出库/收款明细 + 期末 */
    @GetMapping("/customer-statement")
    @PreAuthorize("@ss.hasPermission('rpt:statement:query')")
    public CommonResult<Map<String, Object>> customerStatement(
            @RequestParam("partnerId") Long partnerId,
            @RequestParam String from,
            @RequestParam String to) {
        BigDecimal opening = analysisMapper.statementOpeningDebit(partnerId, from)
                .subtract(analysisMapper.statementOpeningCredit(partnerId, from));
        List<Map<String, Object>> rows = new ArrayList<>();
        rows.addAll(analysisMapper.statementOutboundRows(partnerId, from, to));
        rows.addAll(analysisMapper.statementReceiptRows(partnerId, from, to));
        rows.sort(Comparator.comparing(m -> String.valueOf(m.get("date"))));
        BigDecimal balance = opening;
        for (Map<String, Object> row : rows) {
            BigDecimal debit = new BigDecimal(String.valueOf(row.getOrDefault("debit", "0")));
            BigDecimal credit = new BigDecimal(String.valueOf(row.getOrDefault("credit", "0")));
            balance = balance.add(debit).subtract(credit);
            row.put("balance", balance);
            row.put("kind", debit.signum() > 0 ? "销售出库" : "收款");
        }
        Map<String, Object> result = new HashMap<>();
        result.put("opening", opening);
        result.put("rows", rows);
        result.put("closing", balance);
        result.put("totalDebit", rows.stream().map(m -> new BigDecimal(String.valueOf(m.getOrDefault("debit", "0"))))
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        result.put("totalCredit", rows.stream().map(m -> new BigDecimal(String.valueOf(m.getOrDefault("credit", "0"))))
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        return CommonResult.success(result);
    }

    private String[] range(String from, String to) {
        String f = from == null || from.isBlank()
                ? LocalDate.now().withDayOfYear(1).format(D) : from;
        String t = to == null || to.isBlank() ? LocalDate.now().format(D) : to;
        return new String[]{f, t};
    }

    /** 供应商对账单：期初 + 入库/付款明细 + 期末 */
    @GetMapping("/supplier-statement")
    @PreAuthorize("@ss.hasPermission('rpt:statement:query')")
    public CommonResult<Map<String, Object>> supplierStatement(
            @RequestParam("partnerId") Long partnerId,
            @RequestParam String from,
            @RequestParam String to) {
        BigDecimal opening = analysisMapper.supplierOpeningDebit(partnerId, from)
                .subtract(analysisMapper.supplierOpeningCredit(partnerId, from));
        List<Map<String, Object>> rows = new ArrayList<>();
        rows.addAll(analysisMapper.supplierInboundRows(partnerId, from, to));
        rows.addAll(analysisMapper.supplierPaymentRows(partnerId, from, to));
        rows.sort(Comparator.comparing(m -> String.valueOf(m.get("date"))));
        BigDecimal balance = opening;
        for (Map<String, Object> row : rows) {
            BigDecimal debit = new BigDecimal(String.valueOf(row.getOrDefault("debit", "0")));
            BigDecimal credit = new BigDecimal(String.valueOf(row.getOrDefault("credit", "0")));
            balance = balance.add(debit).subtract(credit);
            row.put("balance", balance);
            row.put("kind", debit.signum() > 0 ? "采购入库" : "付款");
        }
        Map<String, Object> result = new HashMap<>();
        result.put("opening", opening);
        result.put("rows", rows);
        result.put("closing", balance);
        result.put("totalDebit", rows.stream().map(x -> new BigDecimal(String.valueOf(x.getOrDefault("debit", "0"))))
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        result.put("totalCredit", rows.stream().map(x -> new BigDecimal(String.valueOf(x.getOrDefault("credit", "0"))))
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        return CommonResult.success(result);
    }

    /** 收发存汇总：期初 = 期末 - 收 + 发 */
    @GetMapping("/stock-flow")
    @PreAuthorize("@ss.hasPermission('rpt:stockflow:query')")
    public CommonResult<List<Map<String, Object>>> stockFlow(
            @RequestParam String from,
            @RequestParam String to) {
        List<Map<String, Object>> rows = analysisMapper.stockFlowSummary(from, to);
        for (Map<String, Object> row : rows) {
            java.math.BigDecimal endQty = new java.math.BigDecimal(String.valueOf(row.get("endQty")));
            java.math.BigDecimal inQty = new java.math.BigDecimal(String.valueOf(row.get("inQty")));
            java.math.BigDecimal outQty = new java.math.BigDecimal(String.valueOf(row.get("outQty")));
            java.math.BigDecimal endAmount = new java.math.BigDecimal(String.valueOf(row.get("endAmount")));
            java.math.BigDecimal inAmount = new java.math.BigDecimal(String.valueOf(row.get("inAmount")));
            java.math.BigDecimal outAmount = new java.math.BigDecimal(String.valueOf(row.get("outAmount")));
            row.put("beginQty", endQty.subtract(inQty).add(outQty));
            row.put("beginAmount", endAmount.subtract(inAmount).add(outAmount));
        }
        return CommonResult.success(rows);
    }
}
