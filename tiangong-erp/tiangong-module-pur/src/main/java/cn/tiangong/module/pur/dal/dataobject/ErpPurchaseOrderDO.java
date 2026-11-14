package cn.tiangong.module.pur.dal.dataobject;

import cn.tiangong.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 采购订单。price 口径：含税单价；M1 期间 total_amount 与 total_amount_tax 相等（税率字段预留）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("erp_purchase_order")
public class ErpPurchaseOrderDO extends BaseDO {

    private String no;

    private Long supplierId;

    private LocalDate orderDate;

    private LocalDate expectDate;

    private BigDecimal totalQty;

    private BigDecimal totalAmount;

    private BigDecimal totalTax;

    private BigDecimal totalAmountTax;

    private String remark;

    private java.math.BigDecimal depositAmount;

    /** 见 BizOrderStatus：10 草稿 30 已审核 40 已完成 50 已作废 */
    private Integer status;

    private Long auditUserId;

    private LocalDateTime auditTime;
}
