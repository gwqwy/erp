package cn.tiangong.module.bas.api;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 基础数据 - 产品对外 API（跨模块只允许走 api 包）。
 */
public interface ProductApi {

    /** 批量取产品名称，不存在的不在结果中 */
    Map<Long, String> getProductNames(Collection<Long> productIds);

    /** 取单个产品名称，不存在返回 "未知产品" */
    String getProductName(Long productId);

    /** 产品是否启用批次管理 */
    boolean isBatchEnabled(Long productId);

    /** 启用中的产品库存上下限（预警用） */
    List<StockLimit> getStockLimits();

    record StockLimit(Long productId, String name, java.math.BigDecimal stockMin, java.math.BigDecimal stockMax) {
    }

    /** 批量取采购价（用于 BOM 成本卷算） */
    java.util.Map<Long, java.math.BigDecimal> getPurchasePrices(java.util.Set<Long> productIds);
}
