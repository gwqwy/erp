package cn.tiangong.module.rpt.dal.mysql;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 经营分析原生聚合查询（只读，跨业务表 join）。
 */
public interface RptAnalysisMapper {

    /** 销售商品排行（已审核订单） */
    @Select("SELECT i.product_id AS productId, p.name AS productName, SUM(i.qty) AS qty, SUM(i.amount) AS amount " +
            "FROM erp_sale_order_item i " +
            "JOIN erp_sale_order o ON i.order_id = o.id " +
            "JOIN erp_product p ON i.product_id = p.id " +
            "WHERE o.deleted = 0 AND o.status >= 30 AND o.order_date BETWEEN #{from} AND #{to} " +
            "GROUP BY i.product_id, p.name ORDER BY amount DESC LIMIT #{limit}")
    List<Map<String, Object>> saleRank(@Param("from") String from, @Param("to") String to, @Param("limit") int limit);

    /** 采购商品排行（已审核订单） */
    @Select("SELECT i.product_id AS productId, p.name AS productName, SUM(i.qty) AS qty, SUM(i.amount) AS amount " +
            "FROM erp_purchase_order_item i " +
            "JOIN erp_purchase_order o ON i.order_id = o.id " +
            "JOIN erp_product p ON i.product_id = p.id " +
            "WHERE o.deleted = 0 AND o.status >= 30 AND o.order_date BETWEEN #{from} AND #{to} " +
            "GROUP BY i.product_id, p.name ORDER BY amount DESC LIMIT #{limit}")
    List<Map<String, Object>> purchaseRank(@Param("from") String from, @Param("to") String to, @Param("limit") int limit);

    /** 库存 ABC 底表：按商品汇总数量与金额（erp_stock_quant 无 deleted 列） */
    @Select("SELECT q.product_id AS productId, p.name AS productName, SUM(q.qty) AS qty, " +
            "SUM(q.qty * IFNULL(q.avg_cost, 0)) AS amount " +
            "FROM erp_stock_quant q JOIN erp_product p ON q.product_id = p.id " +
            "WHERE p.deleted = 0 GROUP BY q.product_id, p.name HAVING amount > 0 ORDER BY amount DESC")
    List<Map<String, Object>> stockAbcBase();

    /** 对账单：出库行（借方） */
    @Select("SELECT outbound_date AS date, no, total_amount AS debit, 0 AS credit " +
            "FROM erp_sale_outbound WHERE customer_id = #{partnerId} AND status >= 30 AND deleted = 0 " +
            "AND outbound_date BETWEEN #{from} AND #{to}")
    List<Map<String, Object>> statementOutboundRows(@Param("partnerId") Long partnerId,
                                                    @Param("from") String from, @Param("to") String to);

    /** 对账单：收款行（贷方） */
    @Select("SELECT receipt_date AS date, no, 0 AS debit, amount AS credit " +
            "FROM erp_finance_receipt WHERE partner_id = #{partnerId} AND status >= 30 AND deleted = 0 " +
            "AND receipt_date BETWEEN #{from} AND #{to}")
    List<Map<String, Object>> statementReceiptRows(@Param("partnerId") Long partnerId,
                                                   @Param("from") String from, @Param("to") String to);

    /** 期初借方：from 之前累计发货 */
    @Select("SELECT IFNULL(SUM(total_amount), 0) FROM erp_sale_outbound " +
            "WHERE customer_id = #{partnerId} AND status >= 30 AND deleted = 0 AND outbound_date < #{before}")
    BigDecimal statementOpeningDebit(@Param("partnerId") Long partnerId, @Param("before") String before);

    /** 期初贷方：from 之前累计回款 */
    @Select("SELECT IFNULL(SUM(amount), 0) FROM erp_finance_receipt " +
            "WHERE partner_id = #{partnerId} AND status >= 30 AND deleted = 0 AND receipt_date < #{before}")
    BigDecimal statementOpeningCredit(@Param("partnerId") Long partnerId, @Param("before") String before);

    /** 低库存商品（预警共用）：总量低于安全下限 */
    @Select("SELECT p.id AS productId, p.name AS productName, p.stock_min AS stockMin, " +
            "IFNULL(SUM(q.qty), 0) AS totalQty " +
            "FROM erp_product p LEFT JOIN erp_stock_quant q ON q.product_id = p.id AND q.deleted = 0 " +
            "WHERE p.deleted = 0 AND p.stock_min IS NOT NULL AND p.stock_min > 0 " +
            "GROUP BY p.id, p.name, p.stock_min HAVING totalQty < p.stock_min ORDER BY totalQty / p.stock_min ASC")
    List<Map<String, Object>> lowStockProducts();

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
            "  SUM(CASE WHEN direction = -1 THEN -qty ELSE 0 END) AS outQty, " +
            "  SUM(CASE WHEN direction = -1 THEN -IFNULL(amount, 0) ELSE 0 END) AS outAmount " +
            "  FROM erp_stock_move WHERE move_time >= #{from} " +
            "  AND move_time < DATE_ADD(#{to}, INTERVAL 1 DAY) GROUP BY product_id) m ON m.product_id = p.id " +
            "WHERE p.deleted = 0 " +
            "HAVING endQty <> 0 OR inQty <> 0 OR outQty <> 0 ORDER BY p.no")
    List<Map<String, Object>> stockFlowSummary(@Param("from") String from, @Param("to") String to);
}
