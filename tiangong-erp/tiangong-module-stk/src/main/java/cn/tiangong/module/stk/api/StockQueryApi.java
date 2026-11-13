package cn.tiangong.module.stk.api;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;

/**
 * 库存查询对外 API（只读）。
 */
public interface StockQueryApi {

    /** 汇总某类业务单据的过账成本金额（如工单累计领料成本） */
    BigDecimal sumMoveAmount(String bizType, Collection<Long> bizIds);

    /** 按仓库取产品当前结存（缺位视为 0） */
    Map<Long, BigDecimal> getQuantQty(Long warehouseId, Collection<Long> productIds);

    /** 全部即时库存总金额（Σ qty × avg_cost） */
    BigDecimal totalStockValue();
}
