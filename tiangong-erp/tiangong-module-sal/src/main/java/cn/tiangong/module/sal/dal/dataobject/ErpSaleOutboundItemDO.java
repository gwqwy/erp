package cn.tiangong.module.sal.dal.dataobject;

import cn.tiangong.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 销售出库明细。outbound 后成本金额由库存引擎按移动加权平均成本确定。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("erp_sale_outbound_item")
public class ErpSaleOutboundItemDO extends BaseDO {

    private Long outboundId;

    private Long orderItemId;

    private Long productId;

    private Long unitId;

    private BigDecimal qty;

    private BigDecimal price;

    private BigDecimal amount;

    private String remark;
}
