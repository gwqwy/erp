package cn.tiangong.module.stk.api.dto;

import java.math.BigDecimal;

/**
 * 库存过账指令：业务模块将单据审核结果翻译为指令，提交给 StockPostingApi 统一过账。
 *
 * @param bizType    业务类型，如 purchase_in / sale_out
 * @param bizId      来源单据 id
 * @param bizNo      来源单号
 * @param direction  1 入库 / -1 出库
 * @param warehouseId 仓库
 * @param productId  产品
 * @param qty        基本单位数量（正数）
 * @param price      入库成本价（出库时忽略，由引擎取当时移动加权平均成本）
 * @param partnerId  交易对手（供应商/客户）
 * @param batchNo    批次（可空）
 */
public record MoveCommand(
        String bizType,
        Long bizId,
        String bizNo,
        Integer direction,
        Long warehouseId,
        Long productId,
        BigDecimal qty,
        BigDecimal price,
        Long partnerId,
        String batchNo) {
}
