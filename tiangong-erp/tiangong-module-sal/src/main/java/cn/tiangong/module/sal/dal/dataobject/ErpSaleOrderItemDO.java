package cn.tiangong.module.sal.dal.dataobject;

import cn.tiangong.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 销售订单明细。outbound_qty 由出库单审核时回写。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("erp_sale_order_item")
public class ErpSaleOrderItemDO extends BaseDO {

    private Long orderId;

    private Long productId;

    private Long unitId;

    private BigDecimal qty;

    private BigDecimal price;

    private BigDecimal amount;

    private BigDecimal taxRate;

    private BigDecimal outboundQty;

    private String remark;
}
