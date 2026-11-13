package cn.tiangong.module.stk.dal.mysql;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 库存预警原生查询 / 站内消息写入（避免跨模块依赖 system）。
 */
public interface StockAlertMapper {

    /** 低库存商品：总量低于安全下限（erp_stock_quant 无 deleted 列） */
    @Select("SELECT p.id AS productId, p.no AS productNo, p.name AS productName, p.stock_min AS stockMin, " +
            "IFNULL(SUM(q.qty), 0) AS totalQty " +
            "FROM erp_product p LEFT JOIN erp_stock_quant q ON q.product_id = p.id " +
            "WHERE p.deleted = 0 AND p.stock_min IS NOT NULL AND p.stock_min > 0 " +
            "GROUP BY p.id, p.no, p.name, p.stock_min HAVING totalQty < p.stock_min " +
            "ORDER BY totalQty / p.stock_min ASC")
    List<Map<String, Object>> lowStockProducts();

    /** 写站内消息 */
    @Insert("INSERT INTO system_message (user_id, title, content, read_flag, creator) " +
            "VALUES (#{userId}, #{title}, #{content}, 0, 'system')")
    int insertMessage(@Param("userId") Long userId, @Param("title") String title, @Param("content") String content);

    /** 通知对象：最小的用户 id（超级管理员） */
    @Select("SELECT id FROM system_users WHERE deleted = 0 ORDER BY id ASC LIMIT 1")
    Long firstUserId();
}
