package cn.tiangong.module.pur.dal.dataobject;

import cn.tiangong.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 采购退货明细。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("erp_purchase_return_item")
public class ErpPurchaseReturnItemDO extends BaseDO {

    private Long returnId;

    private Long productId;

    private BigDecimal qty;

    private BigDecimal price;

    private String remark;
}
