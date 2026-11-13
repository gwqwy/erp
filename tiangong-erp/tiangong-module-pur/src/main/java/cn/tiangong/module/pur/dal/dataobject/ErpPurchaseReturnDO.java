package cn.tiangong.module.pur.dal.dataobject;

import cn.tiangong.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 采购退货单：审核后货物出库（走库存引擎），凭证经事件生成。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("erp_purchase_return")
public class ErpPurchaseReturnDO extends BaseDO {

    private String no;

    private Long supplierId;

    private Long warehouseId;

    private Long orderId;

    private LocalDate returnDate;

    private BigDecimal totalQty;

    private BigDecimal totalAmount;

    private String remark;

    private Integer status;

    private Long auditUserId;

    private LocalDateTime auditTime;
}
