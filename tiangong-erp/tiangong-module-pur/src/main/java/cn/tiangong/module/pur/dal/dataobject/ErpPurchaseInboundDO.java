package cn.tiangong.module.pur.dal.dataobject;

import cn.tiangong.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 采购入库单：orderId 为空表示无来源直接入库；审核时经库存引擎过账（direction=1）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("erp_purchase_inbound")
public class ErpPurchaseInboundDO extends BaseDO {

    private String no;

    private Long supplierId;

    private Long warehouseId;

    /** 来源采购订单，可空 */
    private Long orderId;

    private LocalDate inboundDate;

    private BigDecimal totalQty;

    private BigDecimal totalAmount;

    private String remark;

    private Integer status;

    private Long auditUserId;

    private LocalDateTime auditTime;
}
