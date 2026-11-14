package cn.tiangong.module.stk.api;

import java.util.List;

/**
 * 序列号追溯 API：入库/出库审核时登记序列号（采购/销售模块调用）。
 */
public interface SerialTrackApi {

    /** 入库登记（status 在库）；已存在的序列号跳过 */
    void registerInbound(List<String> serialNos, Long productId, Long warehouseId, String inboundNo);

    /** 出库核销（在库 → 已出库）；未登记的自动补记后直接置为已出库 */
    void consumeOutbound(List<String> serialNos, Long productId, Long warehouseId, String outboundNo);
}
