import fs from 'node:fs';

// ================= 后端：RptAnalysisMapper 追加 =================

const mapperPath = 'tiangong-erp/tiangong-module-rpt/src/main/java/cn/tiangong/module/rpt/dal/mysql/RptAnalysisMapper.java';
let m = fs.readFileSync(mapperPath, 'utf8');
if (!m.includes('supplierInboundRows')) {
  m = m.trimEnd().replace(/\n}\n?$/, `

    // ========== V16：供应商对账单 / 收发存汇总 ==========

    /** 对账单：采购入库行（借方 = 应付增加） */
    @Select("SELECT inbound_date AS date, no, total_amount AS debit, 0 AS credit " +
            "FROM erp_purchase_inbound WHERE supplier_id = #{partnerId} AND status >= 30 AND deleted = 0 " +
            "AND inbound_date BETWEEN #{from} AND #{to}")
    List<Map<String, Object>> supplierInboundRows(@Param("partnerId") Long partnerId,
                                                  @Param("from") String from, @Param("to") String to);

    /** 对账单：付款行（贷方） */
    @Select("SELECT payment_date AS date, no, 0 AS debit, amount AS credit " +
            "FROM erp_finance_payment WHERE partner_id = #{partnerId} AND status >= 30 AND deleted = 0 " +
            "AND payment_date BETWEEN #{from} AND #{to}")
    List<Map<String, Object>> supplierPaymentRows(@Param("partnerId") Long partnerId,
                                                  @Param("from") String from, @Param("to") String to);

    /** 期初借方：from 之前累计入库 */
    @Select("SELECT IFNULL(SUM(total_amount), 0) FROM erp_purchase_inbound " +
            "WHERE supplier_id = #{partnerId} AND status >= 30 AND deleted = 0 AND inbound_date < #{before}")
    BigDecimal supplierOpeningDebit(@Param("partnerId") Long partnerId, @Param("before") String before);

    /** 期初贷方：from 之前累计付款 */
    @Select("SELECT IFNULL(SUM(amount), 0) FROM erp_finance_payment " +
            "WHERE partner_id = #{partnerId} AND status >= 30 AND deleted = 0 AND payment_date < #{before}")
    BigDecimal supplierOpeningCredit(@Param("partnerId") Long partnerId, @Param("before") String before);

    /** 收发存汇总：期末结存 + 本期收发（erp_stock_quant / erp_stock_move 均无 deleted 列） */
    @Select("SELECT p.id AS productId, p.no AS productNo, p.name AS productName, " +
            "IFNULL(qs.qty, 0) AS endQty, IFNULL(qs.amount, 0) AS endAmount, " +
            "IFNULL(m.inQty, 0) AS inQty, IFNULL(m.inAmount, 0) AS inAmount, " +
            "IFNULL(m.outQty, 0) AS outQty, IFNULL(m.outAmount, 0) AS outAmount " +
            "FROM erp_product p " +
            "LEFT JOIN (SELECT product_id, SUM(qty) qty, SUM(qty * IFNULL(avg_cost, 0)) amount " +
            "  FROM erp_stock_quant GROUP BY product_id) qs ON qs.product_id = p.id " +
            "LEFT JOIN (SELECT product_id, " +
            "  SUM(CASE WHEN direction = 1 THEN qty ELSE 0 END) AS inQty, " +
            "  SUM(CASE WHEN direction = 1 THEN IFNULL(amount, 0) ELSE 0 END) AS inAmount, " +
            "  SUM(CASE WHEN direction = -1 THEN qty ELSE 0 END) AS outQty, " +
            "  SUM(CASE WHEN direction = -1 THEN IFNULL(amount, 0) ELSE 0 END) AS outAmount " +
            "  FROM erp_stock_move WHERE move_time >= #{from} " +
            "  AND move_time < DATE_ADD(#{to}, INTERVAL 1 DAY) GROUP BY product_id) m ON m.product_id = p.id " +
            "WHERE p.deleted = 0 " +
            "HAVING endQty <> 0 OR inQty <> 0 OR outQty <> 0 ORDER BY p.no")
    List<Map<String, Object>> stockFlowSummary(@Param("from") String from, @Param("to") String to);
}
`);
  fs.writeFileSync(mapperPath, m, 'utf8');
}
console.log('mapper extended:', m.includes('stockFlowSummary'));

// ================= 后端：RptAnalysisController 追加 =================

const ctrlPath = 'tiangong-erp/tiangong-module-rpt/src/main/java/cn/tiangong/module/rpt/controller/admin/RptAnalysisController.java';
let c = fs.readFileSync(ctrlPath, 'utf8');
if (!c.includes('supplierStatement')) {
  c = c.trimEnd().replace(/\n}\n?$/, `

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
`);
  fs.writeFileSync(ctrlPath, c, 'utf8');
}
console.log('controller extended:', c.includes('stockFlow'));

// ================= SQL V16 =================

fs.writeFileSync('tiangong-erp/tiangong-server/src/main/resources/db/migration/V16__stockflow_supplier_message.sql', `-- =====================================================================
-- V16：收发存汇总 / 供应商对账单 / 消息中心 菜单
-- =====================================================================

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, component) VALUES
  (15220, '收发存汇总', '', 2, 14, 15010, 'stock-flow', 'report/stock-flow/index'),
  (15221, '收发存查询', 'rpt:stockflow:query', 3, 1, 15220, '', ''),
  (15222, '供应商对账单', '', 2, 15, 15010, 'supplier-statement', 'report/supplier-statement/index'),
  (15223, '对账单查看', 'rpt:statement:query', 3, 1, 15222, '', ''),
  (15224, '消息中心', '', 2, 21, 1, 'message', 'system/message/index');
`, 'utf8');
console.log('V16 sql written');

console.log('gen-v16-backend done');
