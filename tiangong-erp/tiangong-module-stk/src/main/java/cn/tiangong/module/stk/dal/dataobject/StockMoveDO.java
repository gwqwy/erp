package cn.tiangong.module.stk.dal.dataobject;

import cn.tiangong.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 库存流水账（借鉴 Odoo stock.move）：只增不改，是审计与追溯的事实。
 * qty 带方向：正数入库、负数出库；换算为基本单位。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("erp_stock_move")
public class StockMoveDO extends BaseDO {

    private String moveNo;

    /** 业务类型：purchase_in / sale_out / produce_in / material_out / other_in / other_out… */
    private String bizType;

    private Long bizId;

    private String bizNo;

    /** 1 入库 / -1 出库 */
    private Integer direction;

    private Long warehouseId;

    /** 库位，M1 默认 0（默认库位） */
    private Long locationId;

    private Long productId;

    private Long unitId;

    /** 基本单位数量，带方向 */
    private BigDecimal qty;

    /** 成本单价：入库=采购价，出库=当时的移动加权平均成本 */
    private BigDecimal price;

    private BigDecimal amount;

    private String batchNo;

    private Long partnerId;

    private LocalDateTime moveTime;

    private Long postUserId;
}
