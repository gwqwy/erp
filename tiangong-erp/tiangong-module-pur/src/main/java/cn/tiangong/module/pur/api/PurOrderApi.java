package cn.tiangong.module.pur.api;

import java.math.BigDecimal;
import java.util.List;

/**
 * 采购 - 对外 API（供 MRP 计划转采购等场景）。
 */
public interface PurOrderApi {

    /**
     * 创建采购订单草稿。
     *
     * @param supplierId 供应商
     * @param remark     备注
     * @param items      明细（productId, qty, price 可为 0 待询价）
     * @return 采购订单 id
     */
    Long createDraftOrder(Long supplierId, String remark,
                          List<Item> items);

    record Item(Long productId, Long unitId, BigDecimal qty, BigDecimal price, String remark) {
    }
}
