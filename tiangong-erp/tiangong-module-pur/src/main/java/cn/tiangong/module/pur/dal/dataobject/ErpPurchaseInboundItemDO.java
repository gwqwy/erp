package cn.tiangong.module.pur.dal.dataobject;

import cn.tiangong.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 采购入库明细：order_item_id 可空（无来源直接入库）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("erp_purchase_inbound_item")
public class ErpPurchaseInboundItemDO extends BaseDO {

    private Long inboundId;

    private Long orderItemId;

    private Long productId;

    private Long unitId;

    private BigDecimal qty;

    private BigDecimal price;

    private BigDecimal amount;

    private String remark;
}
