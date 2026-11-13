package cn.tiangong.module.sal.api;

import java.math.BigDecimal;
import java.util.List;

/**
 * 销售 - 对外 API（供 CRM 商机转单等场景）。
 */
public interface SaleOrderApi {

    /** 创建销售订单草稿 */
    Long createDraftOrder(Long customerId, String remark, List<Item> items);

    record Item(Long productId, Long unitId, BigDecimal qty, BigDecimal price, String remark) {
    }
}
