package cn.tiangong.module.pur.dal.dataobject;

import cn.tiangong.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 采购订单明细。inbound_qty 由入库单审核时回写，用于执行进度与行级关闭。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("erp_purchase_order_item")
public class ErpPurchaseOrderItemDO extends BaseDO {

    private Long orderId;

    private Long productId;

    private Long unitId;

    private BigDecimal qty;

    private BigDecimal price;

    private BigDecimal amount;

    private BigDecimal taxRate;

    private BigDecimal inboundQty;

    private String remark;
}
